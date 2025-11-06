package com.example.app.repository;

import com.example.app.domain.MemberVO;
import com.example.app.dto.MemberDTO;
import com.example.app.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberDAO {
    private final MemberMapper memberMapper;

//    회원가입
    public void save(MemberVO memberVO){
        memberMapper.insert(memberVO);
    }

//    로그인
    public Optional<MemberDTO> findForLogin(MemberDTO memberDTO){
        return memberMapper.selectForLogin(memberDTO);
    }

//    이메일로 회원 조회
    public Optional<MemberDTO> findByMemberEmail(String memberEmail){
        return memberMapper.selectMemberByMemberEmail(memberEmail);
    }
}
