package com.example.rentapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WebSocketMessageDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderLogin;
    private String text;
    private LocalDateTime sentAt;
    private Long premiseId;
    private String type;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String deliveryStatus;  // НОВОЕ ПОЛЕ

    public WebSocketMessageDto(Long id, Long senderId, Long receiverId, String senderLogin,
                               String text, LocalDateTime sentAt, Long premiseId, String type) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.sentAt = sentAt;
        this.premiseId = premiseId;
        this.type = type;
        this.deliveryStatus = "DELIVERED";
    }

    public WebSocketMessageDto(Long id, Long senderId, Long receiverId, String senderLogin,
                               String text, LocalDateTime sentAt, Long premiseId, String type,
                               String fileName, String fileUrl, String fileType, Long fileSize) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.sentAt = sentAt;
        this.premiseId = premiseId;
        this.type = type;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.deliveryStatus = "DELIVERED";
    }
}