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
    private String type; // "MESSAGE", "SYSTEM", "TYPING", "READ_RECEIPT", "FILE"

    // Поля для файлов
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;

    // Конструктор для обычных сообщений (без файлов)
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
        this.fileName = null;
        this.fileUrl = null;
        this.fileType = null;
        this.fileSize = null;
    }

    // Конструктор для файловых сообщений
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
    }
}