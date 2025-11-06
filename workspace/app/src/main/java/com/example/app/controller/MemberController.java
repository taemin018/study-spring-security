package com.example.app.controller;

import com.example.app.auth.CustomUserDetails;
import com.example.app.dto.MemberDTO;
import com.example.app.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/member/**")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

//    회원가입
    @GetMapping("join")
    public String join(MemberDTO memberDTO, Model model) {
        model.addAttribute("memberDTO", memberDTO);
        return "member/join";
    }

    @PostMapping("join")
    public RedirectView join(MemberDTO memberDTO) {
        memberService.join(memberDTO);
        return new RedirectView("member/login");
    }

    //    로그인
    @GetMapping("login")
    public String login(MemberDTO memberDTO, Model model,
                        @CookieValue(value = "remember", required = false) boolean remember,
                        @CookieValue(value = "rememberEmail", required = false) String rememberEmail) {
        memberDTO.setMemberEmail(rememberEmail);
        model.addAttribute("memberDTO", memberDTO);
        model.addAttribute("remember", remember);
        return "member/login";
    }
}
