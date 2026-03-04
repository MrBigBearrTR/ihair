package com.bigbear.ihair.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DenemeController {

    @GetMapping("/deneme")
    public String deneme() {
        return "başarılı";
    }
}
