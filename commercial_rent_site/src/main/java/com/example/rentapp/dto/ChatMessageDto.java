package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для сообщений чата
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long id;

    private Long senderId;

    private Long receiverId;

    private String senderLogin;   // Логин отправителя для отображения

    private String text;          // Текст сообщения

    private LocalDateTime sentAt; // Время отправки

    private boolean read;         // Прочитано ли сообщение

    // Конструктор для удобства создания из сущности
    public ChatMessageDto(Long senderId, Long receiverId, String senderLogin, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.sentAt = LocalDateTime.now();
        this.read = false;
    }
}