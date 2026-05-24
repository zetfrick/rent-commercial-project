package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderLogin;
    private String text;
    private LocalDateTime sentAt;
    private boolean read;
    private String deliveryStatus;  // НОВОЕ ПОЛЕ

    public ChatMessageDto(Long senderId, Long receiverId, String senderLogin, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.sentAt = LocalDateTime.now();
        this.read = false;
        this.deliveryStatus = "DELIVERED";
    }
}