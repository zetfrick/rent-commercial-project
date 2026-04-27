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

    private String senderLogin;   // для удобства отображения
    private String text;

    private LocalDateTime sentAt = LocalDateTime.now();

    private boolean read = false;

    // НОВОЕ ПОЛЕ: ID объявления, к которому относится сообщение
    private Long premiseId;

    public ChatMessage(Long senderId, Long receiverId, String senderLogin, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
    }

    public ChatMessage(Long senderId, Long receiverId, String senderLogin, String text, Long premiseId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderLogin = senderLogin;
        this.text = text;
        this.premiseId = premiseId;
    }
}