package com.example.app.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HttpServletResponse response;
    private final UserDetailsService userDetailService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

//    액세스 토큰 생성
    public String createAccessToken(String username) {
        long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30;
        String accessToken = Jwts.builder()
//                토큰 소유자
                .setSubject(username)
//                토큰에 저장할 정보
                .claim("memberEmail", username)
//                발급 시간
                .setIssuedAt(new Date())
//                만료 시간
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 10); // 10분
        response.addCookie(accessTokenCookie);

        return accessToken;
    }
//    JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key).build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
//            ExpiredJwtException: 토큰 만료
//            UnsupportedJwtException: 지원하지 않는 JWT 형식
//            MalformedJwtException: JWT 형식이 잘못되었을 때
//            SignatureException: 서명이 일치하지 않을 때(위조된 토큰)
//            IllegalArgumentException: null, 빈 문자열 등 잘못된 파라미터일 때
            return false;
        }
    }

//    인증 정보 객체 생성
    public Authentication getAuthentication(String token) {
        String username = getUserName(token);
//        DB 조회
        UserDetails userDetails = userDetailService.loadUserByUsername(username);
//        Spring Security가 이해할 수 있는 Authentication 객체로 감싸기
//        JWT 인증에서는 이미 토큰 자체가 인증 수단이기 때문에 비밀번호를 ""로 전달해도 아무 문제 없다.
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

//    토큰 소유자 추출
    public String getUserName(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

//    리프레시 토큰 생성
    public String createRefreshToken(String username) {
        long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 1;
        String refreshToken = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + username,
                refreshToken,
                REFRESH_TOKEN_VALIDITY,
                TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }
//    리프레시 토큰 삭제
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }

//    입력 토큰 일치 여부 확인
    public boolean isRefreshTokenValid(String username, String token) {
        String redisRefreshToken = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
        return token.equals(redisRefreshToken);
    }

//    JWT 토큰에서 클레임 정보 추출
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

//    클라이언트 요청에서 액세스 토큰 추출
    public String parseTokenFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
//        Authorization: Bearer fnmWsEiofBMIO029hfinDEo...
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }

        if(request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if("accessToken".equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }

        return null;
    }


//    로그아웃 블랙 리스트 메소드
    public void addToBlacklist(String token){
        try {
            String tokenId = getBlackListTokenKey(token);
            Claims claims = getClaims(token);
            long gap = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (gap > 0) {
                String blackListKey = BLACKLIST_PREFIX + tokenId;
                redisTemplate.opsForValue().set(
                        blackListKey,
                        "blacklisted",
                        gap,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰을 블랙리스트에 추가하는데에 실패했습니다.");
        }

    }

//    블랙 리스트 조회
    public boolean isTokenBlackList(String token){
        try {
            String tokenId = getBlackListTokenKey(token);
            String blacklistKey = BLACKLIST_PREFIX + tokenId;
            Boolean hasKey = redisTemplate.hasKey(blacklistKey);
            return hasKey;
        } catch (Exception e) {
            log.error("블랙리스트 확인 중 오류 발생: {}", e.getMessage());
            return true;
        }
    }

//    MD5 해시값 생성
    private String getBlackListTokenKey(String token) {
//        이렇게 하면 긴 토큰 문자열을 짧고 고유한 키로 관리 가능(메모리 효율)
        return DigestUtils.md5DigestAsHex(token.getBytes());
    }
}
















