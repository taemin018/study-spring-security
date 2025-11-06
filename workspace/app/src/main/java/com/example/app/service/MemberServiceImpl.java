package com.example.app.service;

import com.example.app.common.exception.MemberLoginFailException;
import com.example.app.dto.MemberDTO;
import com.example.app.repository.MemberDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberDAO memberDAO;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void join(MemberDTO memberDTO) {
        memberDTO.setMemberPassword(passwordEncoder.encode(memberDTO.getMemberPassword()));
        memberDAO.save(toVO(memberDTO));
    }

    @Override
    public MemberDTO login(MemberDTO memberDTO) {
        return memberDAO.findForLogin(memberDTO).orElseThrow(MemberLoginFailException::new);
    }
}
