package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;  // Кому принадлежит уведомление

    @Column(nullable = false)
    private String type;

    private Long relatedId;  // ID связанного объекта (чата, помещения, бронирования, комментария)

    private Long fromUserId;  // От кого (если есть)

    private String fromUserName;  // Имя отправителя

    @Column(length = 2000)
    private String content;  // Текст уведомления

    private String link;  // Ссылка для перехода

    private boolean read = false;  // Прочитано/не прочитано

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Конструктор для быстрого создания
    public Notification(Long userId, String type, Long relatedId, Long fromUserId,
                        String fromUserName, String content, String link) {
        this.userId = userId;
        this.type = type;
        this.relatedId = relatedId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.content = content;
        this.link = link;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
}