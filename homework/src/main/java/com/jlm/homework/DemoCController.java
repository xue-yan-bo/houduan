package com.jlm.homework;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoCController {


    @GetMapping("/hello")
    public String hello() {
        return "Hello";
    }

}
