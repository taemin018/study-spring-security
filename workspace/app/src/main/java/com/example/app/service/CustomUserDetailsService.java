package com.example.app.service;

import com.example.app.auth.CustomUserDetails;
import com.example.app.dto.MemberDTO;
import com.example.app.repository.MemberDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberDAO memberDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        이메일로 전체 정보 조회
        MemberDTO memberDTO = memberDAO.findByMemberEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("소유자를 찾을 수 없습니다."));
        return new CustomUserDetails(memberDTO);
    }
}
