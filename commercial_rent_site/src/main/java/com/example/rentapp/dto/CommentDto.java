package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для комментариев к объявлению
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;

    private Long premiseId;

    private String authorName;        // Логин пользователя, оставившего комментарий

    private String text;              // Текст комментария

    private LocalDateTime createdAt;  // Дата и время создания

    // Конструктор для удобства создания из контроллера
    public CommentDto(Long premiseId, String authorName, String text) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}