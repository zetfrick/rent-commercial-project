// src/main/java/com/example/rentapp/controller/api/ApiAuthController.java
package com.example.rentapp.controller.api;

import com.example.rentapp.dto.RegisterRequest;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Получение информации о текущем авторизованном пользователе
     * Используется для WebSocket подключения
     */
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails != null) {
            User user = userService.findByLogin(userDetails.getUsername()).orElse(null);
            if (user != null) {
                response.put("id", user.getId());
                response.put("login", user.getLogin());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("role", user.getRole().name());
                return ResponseEntity.ok(response);
            }
        }

        response.put("authenticated", false);
        return ResponseEntity.ok(response);
    }

    /**
     * Проверка авторизации для AJAX-запросов
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAuth(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails != null) {
            response.put("authenticated", true);
            response.put("username", userDetails.getUsername());
            return ResponseEntity.ok(response);
        } else {
            response.put("authenticated", false);
            return ResponseEntity.status(401).body(response);
        }
    }
}