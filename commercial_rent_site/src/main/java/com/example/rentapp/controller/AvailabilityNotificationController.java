package com.example.rentapp.controller;

import com.example.rentapp.dto.AvailabilityNotificationRequestDto;
import com.example.rentapp.entity.AvailabilityNotification;
import com.example.rentapp.entity.User;
import com.example.rentapp.repository.AvailabilityNotificationRepository;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notify-availability")
public class AvailabilityNotificationController {

    @Autowired
    private AvailabilityNotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestNotification(
            @RequestBody AvailabilityNotificationRequestDto request,
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

        boolean exists = notificationRepository.existsByPremiseIdAndUserIdAndStartDateAndEndDate(
                request.getPremiseId(), currentUser.getId(), request.getStartDate(), request.getEndDate());

        if (exists) {
            response.put("success", false);
            response.put("message", "Вы уже подписаны на уведомление об этих датах");
            return ResponseEntity.badRequest().body(response);
        }

        AvailabilityNotification notification = new AvailabilityNotification(
                currentUser.getId(),
                request.getPremiseId(),
                request.getStartDate(),
                request.getEndDate()
        );

        notificationRepository.save(notification);

        response.put("success", true);
        response.put("message", "Уведомление создано");
        return ResponseEntity.ok(response);
    }
}