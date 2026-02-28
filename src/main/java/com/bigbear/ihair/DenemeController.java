package com.bigbear.ihair;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DenemeController {

    @GetMapping("/deneme")
    public String deneme() {
        return "başarılı";
    }
}

