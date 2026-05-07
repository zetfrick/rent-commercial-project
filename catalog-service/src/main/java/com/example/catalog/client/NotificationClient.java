package com.example.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ВРЕМЕННО используем прямой URL для теста
@FeignClient(name = "main-service", url = "http://localhost:8080")
public interface NotificationClient {

    @PostMapping("/notifications/api/create")
    void createNotification(
            @RequestParam("userId") Long userId,
            @RequestParam("type") String type,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @RequestParam(value = "fromUserId", required = false) Long fromUserId,
            @RequestParam(value = "fromUserName", required = false) String fromUserName,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam("link") String link
    );
}