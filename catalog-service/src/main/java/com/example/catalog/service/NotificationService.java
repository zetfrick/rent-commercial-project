package com.example.catalog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${main.service.url:http://localhost:8080}")
    private String mainServiceUrl;

    public void sendNotification(Long userId, String type, Long relatedId,
                                 Long fromUserId, String fromUserName,
                                 String content, String link) {

        int maxRetries = 3;
        int retryDelay = 2000; // 2 секунды между попытками

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Строим URL с кодированием параметров
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mainServiceUrl + "/notifications/api/create")
                        .queryParam("userId", userId)
                        .queryParam("type", type)
                        .queryParam("link", link);

                if (relatedId != null) {
                    builder.queryParam("relatedId", relatedId);
                }
                if (fromUserId != null) {
                    builder.queryParam("fromUserId", fromUserId);
                }
                if (fromUserName != null) {
                    builder.queryParam("fromUserName", fromUserName);
                }
                if (content != null) {
                    String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
                    builder.queryParam("content", encodedContent);
                }

                String url = builder.toUriString();
                System.out.println("→ Отправка REST запроса (попытка " + attempt + " из " + maxRetries + "): " + url);

                String response = restTemplate.getForObject(url, String.class);
                System.out.println("✓ Уведомление успешно отправлено через RestTemplate");
                if (response != null && !response.isEmpty()) {
                    System.out.println("  Response: " + response.substring(0, Math.min(response.length(), 200)));
                }
                return; // Успех - выходим из метода

            } catch (Exception e) {
                System.err.println("✗ Ошибка отправки уведомления (попытка " + attempt + " из " + maxRetries + "): " + e.getMessage());

                if (attempt < maxRetries) {
                    System.out.println("  Повторная попытка через " + retryDelay + " мс...");
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Прервано ожидание между попытками");
                        throw new RuntimeException("Interrupted while retrying", ie);
                    }
                } else {
                    System.err.println("❌ Не удалось отправить уведомление после " + maxRetries + " попыток");
                    e.printStackTrace();
                    throw new RuntimeException("Failed to send notification after " + maxRetries + " attempts", e);
                }
            }
        }
    }
}