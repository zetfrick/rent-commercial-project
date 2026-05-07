package com.example.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private Long premiseId;
    private String authorName;
    private Long authorId;
    private String text;
    private LocalDateTime createdAt;

    // Конструктор для создания комментария без ID и даты
    public CommentDto(Long premiseId, String authorName, String text) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}