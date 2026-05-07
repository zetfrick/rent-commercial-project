package com.example.rentapp.controller;

import com.example.rentapp.dto.NotificationDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.NotificationService;
import com.example.rentapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String notificationsPage(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(required = false) String tab,
                                    @RequestParam(required = false) String city,
                                    jakarta.servlet.http.HttpServletRequest request,
                                    Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User currentUser = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentTab", tab != null ? tab : "unread");

        return "future/notifications";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Пользователь не найден"));
        }

        Map<String, Object> response = new HashMap<>();

        if ("read".equals(type)) {
            response.put("notifications", notificationService.getReadNotifications(currentUser.getId()));
        } else {
            response.put("notifications", notificationService.getUnreadNotifications(currentUser.getId()));
        }

        response.put("unreadCount", notificationService.getUnreadNotificationsCount(currentUser.getId()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createNotificationGet(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam(required = false) Long relatedId,
            @RequestParam(required = false) Long fromUserId,
            @RequestParam(required = false) String fromUserName,
            @RequestParam(required = false) String content,
            @RequestParam String link) {

        String decodedContent = content;
        if (content != null && content.contains("%")) {
            try {
                decodedContent = URLDecoder.decode(content, StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                System.err.println("Ошибка декодирования content: " + e.getMessage());
            }
        }

        try {
            notificationService.createNotification(userId, type, relatedId, fromUserId, fromUserName, decodedContent, link);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            System.err.println("Ошибка при создании уведомления: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, List<Integer>> request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Не авторизован"));
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Пользователь не найден"));
        }

        List<Integer> idsInt = request.get("ids");
        if (idsInt != null && !idsInt.isEmpty()) {
            List<Long> ids = idsInt.stream().map(Long::valueOf).collect(Collectors.toList());
            notificationService.markAsRead(currentUser.getId(), ids);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Не авторизован"));
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Пользователь не найден"));
        }

        notificationService.markAllAsRead(currentUser.getId());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, List<Integer>> request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Не авторизован"));
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Пользователь не найден"));
        }

        List<Integer> idsInt = request.get("ids");
        if (idsInt != null && !idsInt.isEmpty()) {
            List<Long> ids = idsInt.stream().map(Long::valueOf).collect(Collectors.toList());
            notificationService.deleteNotifications(currentUser.getId(), ids);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/clear-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearReadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Не авторизован"));
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Пользователь не найден"));
        }

        notificationService.clearReadNotifications(currentUser.getId());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createNotificationPost(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam(required = false) Long relatedId,
            @RequestParam(required = false) Long fromUserId,
            @RequestParam(required = false) String fromUserName,
            @RequestParam(required = false) String content,
            @RequestParam String link) {

        try {
            notificationService.createNotification(userId, type, relatedId, fromUserId, fromUserName, content, link);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            System.err.println("Ошибка при создании уведомления: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}