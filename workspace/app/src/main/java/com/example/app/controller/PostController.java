package com.example.app.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/post/**")
@Slf4j
public class PostController {
    @GetMapping("list/{page}")
    public String list(@PathVariable int page){
        log.info("{}", page);
        return "test";
    }
}
