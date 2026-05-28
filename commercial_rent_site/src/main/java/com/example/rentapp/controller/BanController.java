package com.example.rentapp.controller;

import com.example.rentapp.dto.BanRequestDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.entity.UserBan;
import com.example.rentapp.service.UserBanService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/bans")
public class BanController {

    @Autowired
    private UserBanService userBanService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/ban")
    public ResponseEntity<Map<String, Object>> banUser(
            @RequestBody BanRequestDto banRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        System.out.println("=== BanController.banUser ===");
        System.out.println("BanRequest: " + banRequest);
        System.out.println("userId: " + (banRequest != null ? banRequest.getUserId() : "null"));

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return ResponseEntity.status(401).body(response);
        }

        if (banRequest == null || banRequest.getUserId() == null) {
            response.put("success", false);
            response.put("message", "ID пользователя не указан");
            return ResponseEntity.badRequest().body(response);
        }

        User admin = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (admin == null || (admin.getRole() != User.Role.ADMIN && admin.getRole() != User.Role.SUPER_ADMIN)) {
            response.put("success", false);
            response.put("message", "Недостаточно прав");
            return ResponseEntity.status(403).body(response);
        }

        boolean success = userBanService.banUser(admin.getId(), banRequest);

        if (success) {
            try {
                // Вызываем метод снятия объявлений через RestTemplate или Feign клиент
                String catalogUrl = "http://localhost:8081/api/internal/users/" + banRequest.getUserId() + "/unpublish-all";
                restTemplate.postForEntity(catalogUrl, null, Void.class);
                System.out.println("✓ Объявления пользователя #" + banRequest.getUserId() + " сняты с публикации");
            } catch (Exception e) {
                System.err.println("✗ Ошибка при снятии объявлений: " + e.getMessage());
            }
        }

        response.put("success", success);
        if (!success) {
            response.put("message", "Не удалось заблокировать пользователя");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/unban/{userId}")
    public ResponseEntity<Map<String, Object>> unbanUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return ResponseEntity.status(401).body(response);
        }

        if (userId == null) {
            response.put("success", false);
            response.put("message", "ID пользователя не указан");
            return ResponseEntity.badRequest().body(response);
        }

        User admin = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (admin == null || (admin.getRole() != User.Role.ADMIN && admin.getRole() != User.Role.SUPER_ADMIN)) {
            response.put("success", false);
            response.put("message", "Недостаточно прав");
            return ResponseEntity.status(403).body(response);
        }

        boolean success = userBanService.unbanUser(userId, admin.getId());
        response.put("success", success);
        if (!success) {
            response.put("message", "Пользователь не заблокирован");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkBan(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        boolean isBanned = userBanService.isUserBanned(userId);
        response.put("banned", isBanned);

        if (isBanned) {
            Optional<UserBan> ban = userBanService.getActiveBan(userId);
            ban.ifPresent(b -> {
                response.put("reason", b.getReason());
                response.put("bannedUntil", b.getBannedUntil());
            });
        }

        return ResponseEntity.ok(response);
    }
}