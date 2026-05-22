package com.example.rentapp.controller.api;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.RegisterRequest;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CatalogClient catalogClient;

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

    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return ResponseEntity.status(401).body(response);
        }

        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            response.put("success", false);
            response.put("message", "Пароль не указан");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        if (passwordEncoder.matches(password, user.getPassword())) {
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "Неверный пароль");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-email")
    public ResponseEntity<Map<String, Object>> changeEmail(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return ResponseEntity.status(401).body(response);
        }

        String newEmail = request.get("email");
        if (newEmail == null || newEmail.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email не указан");
            return ResponseEntity.badRequest().body(response);
        }

        // Валидация email
        String emailRegex = "^[^\\s@]+@([^\\s@]+\\.)+[^\\s@]+$";
        if (!newEmail.matches(emailRegex)) {
            response.put("success", false);
            response.put("message", "Некорректный email");
            return ResponseEntity.badRequest().body(response);
        }

        // Проверяем, не занят ли email
        if (userService.existsByEmail(newEmail)) {
            User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
            if (currentUser != null && !currentUser.getEmail().equals(newEmail)) {
                response.put("success", false);
                response.put("message", "Этот email уже зарегистрирован");
                return ResponseEntity.badRequest().body(response);
            }
        }

        User user = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        user.setEmail(newEmail);
        userService.save(user);

        // Синхронизируем email в catalog-service
        try {
            Map<String, String> contacts = new HashMap<>();
            contacts.put("email", newEmail);
            catalogClient.updateOwnerContacts(user.getId(), contacts);
            System.out.println("✅ Email синхронизирован с catalog-service для пользователя #" + user.getId());
        } catch (Exception e) {
            System.err.println("❌ Ошибка синхронизации email: " + e.getMessage());
        }

        // ===== ОБНОВЛЯЕМ СЕССИЮ SPRING SECURITY =====
        org.springframework.security.core.userdetails.User updatedUserDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getLogin(),
                        user.getPassword(),
                        userDetails.getAuthorities()
                );

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        updatedUserDetails,
                        userDetails.getPassword(),
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(newAuth);

        httpRequest.getSession().setAttribute(
                SecurityContextHolder.class.getName(),
                SecurityContextHolder.getContext()
        );

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}