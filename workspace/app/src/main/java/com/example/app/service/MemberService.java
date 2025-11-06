package com.example.app.service;

import com.example.app.domain.MemberVO;
import com.example.app.dto.MemberDTO;

public interface MemberService {
//    회원가입
    public void join(MemberDTO memberDTO);

//    로그인
    public MemberDTO login(MemberDTO memberDTO);

    default MemberVO toVO(MemberDTO memberDTO) {
        return MemberVO.builder()
                .id(memberDTO.getId())
                .memberName(memberDTO.getMemberName())
                .memberEmail(memberDTO.getMemberEmail())
                .memberPassword(memberDTO.getMemberPassword())
                .memberStatus(memberDTO.getMemberStatus())
                .memberRole(memberDTO.getMemberRole())
                .createdDate(memberDTO.getCreatedDate())
                .updatedDate(memberDTO.getUpdatedDate())
                .build();
    }
}
