package com.example.rentapp.controller;

import com.example.rentapp.entity.User;
import com.example.rentapp.service.CommentService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentApiController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        // Проверка прав: админ или SUPER_ADMIN могут удалять любые комментарии
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN ||
                currentUser.getRole() == User.Role.SUPER_ADMIN;

        if (!isAdmin) {
            response.put("success", false);
            response.put("message", "Недостаточно прав для удаления комментария");
            return ResponseEntity.status(403).body(response);
        }

        boolean deleted = commentService.deleteComment(id);
        if (deleted) {
            response.put("success", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Не удалось удалить комментарий");
            return ResponseEntity.status(404).body(response);
        }
    }
}