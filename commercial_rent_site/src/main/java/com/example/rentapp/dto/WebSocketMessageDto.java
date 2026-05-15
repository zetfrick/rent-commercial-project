package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderLogin;
    private String text;
    private LocalDateTime sentAt;
    private Long premiseId;
    private String type; // "MESSAGE", "SYSTEM", "TYPING", "READ_RECEIPT"
}