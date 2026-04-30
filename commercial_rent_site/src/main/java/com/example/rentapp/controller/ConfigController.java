// rentapp/src/main/java/com/example/rentapp/controller/ConfigController.java
package com.example.rentapp.controller;

import com.example.rentapp.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @PostMapping("/refresh")
    public String refreshConfig() {
        configService.refreshConfig();
        return "Config refreshed successfully";
    }
}