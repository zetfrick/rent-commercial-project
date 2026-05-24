package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;
    private Long receiverId;

    private String senderLogin;

    @Column(length = 10000)
    private String text;

    private LocalDateTime sentAt = LocalDateTime.now();

    private boolean read = false;

    private Long premiseId;

    @Column(length = 1000)
    private String fileName;

    @Column(length = 1000)
    private String fileUrl;

    @Column(length = 500)
    private String fileType;

    private Long fileSize;

    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'DELIVERED'")
    private String deliveryStatus = "DELIVERED";

    public ChatMessage(Long senderId, Long receiverId, String senderLogin, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.deliveryStatus = "DELIVERED";
    }

    public ChatMessage(Long senderId, Long receiverId, String senderLogin, String text, Long premiseId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.premiseId = premiseId;
        this.deliveryStatus = "DELIVERED";
    }

    public ChatMessage(Long senderId, Long receiverId, String senderLogin, String text, Long premiseId,
                       String fileName, String fileUrl, String fileType, Long fileSize) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.premiseId = premiseId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.deliveryStatus = "DELIVERED";
    }
}