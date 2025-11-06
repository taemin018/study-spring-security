package com.example.app.controller;

import com.example.app.auth.JwtTokenProvider;
import com.example.app.dto.MemberDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/**")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletResponse response;

//    로그인
    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody MemberDTO memberDTO){
        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(memberDTO.getMemberEmail(), memberDTO.getMemberPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtTokenProvider.createAccessToken(((UserDetails) authentication.getPrincipal()).getUsername());
            String refreshToken = jwtTokenProvider.createRefreshToken(((UserDetails) authentication.getPrincipal()).getUsername());

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);

            Cookie rememberEmailCookie = new Cookie("rememberEmail", memberDTO.getMemberEmail());
            Cookie rememberCookie = new Cookie("remember", String.valueOf(memberDTO.isRemember()));

            rememberEmailCookie.setPath("/"); // 모든 경로에서 접근 가능
            rememberCookie.setPath("/"); // 모든 경로에서 접근 가능

            if (memberDTO.isRemember()) {
                // 예: 로그인 이메일을 쿠키에 저장 (민감정보는 피해야 함)
                rememberEmailCookie.setMaxAge(60 * 60 * 24 * 30); // 30일 유지
                response.addCookie(rememberEmailCookie);

                rememberCookie.setMaxAge(60 * 60 * 24 * 30); // 30일 유지
                response.addCookie(rememberCookie);
            } else {
                // 쿠키 삭제
                rememberEmailCookie.setMaxAge(0); // 삭제
                response.addCookie(rememberEmailCookie);

                rememberCookie.setMaxAge(0); // 삭제
                response.addCookie(rememberCookie);
            }
            return ResponseEntity.ok(tokens);

        } catch(AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 실패: " + e.getMessage()));
        }
    }

//    로그아웃
    @PostMapping("logout")
    public void logout() {

    }

//    정보 조회
    @GetMapping("info")
    public MemberDTO getMyInfo(){
        return null;
    }
}
