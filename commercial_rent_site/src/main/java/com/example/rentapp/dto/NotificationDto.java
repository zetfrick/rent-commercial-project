package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private String type;
    private Long relatedId;
    private Long fromUserId;
    private String fromUserName;
    private String content;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationDto(com.example.rentapp.entity.Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.relatedId = notification.getRelatedId();
        this.fromUserId = notification.getFromUserId();
        this.fromUserName = notification.getFromUserName();
        this.content = notification.getContent();
        this.link = notification.getLink();
        this.read = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }
}