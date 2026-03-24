// src/main/java/com/example/rentapp/controller/api/ApiAuthController.java
package com.example.rentapp.controller.api;

import com.example.rentapp.dto.RegisterRequest;
import com.example.rentapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userService.existsByLogin(request.getLogin())) {
            return ResponseEntity.badRequest().body("Логин занят");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email занят");
        }
        userService.registerUser(
                request.getLogin(), request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName()
        );
        return ResponseEntity.ok("Регистрация успешна");
    }
}