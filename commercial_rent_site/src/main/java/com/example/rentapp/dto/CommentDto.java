package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // ПОЛЯ ДЛЯ ОТВЕТОВ
    private Long parentCommentId;
    private Long repliedToUserId;
    private String repliedToUserName;

    // ВЛОЖЕННЫЕ ОТВЕТЫ
    private List<CommentDto> replies = new ArrayList<>();

    public CommentDto(Long premiseId, String authorName, String text) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    // Конструктор для ответа
    public CommentDto(Long premiseId, String authorName, String text,
                      Long parentCommentId, Long repliedToUserId, String repliedToUserName) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.parentCommentId = parentCommentId;
        this.repliedToUserId = repliedToUserId;
        this.repliedToUserName = repliedToUserName;
        this.createdAt = LocalDateTime.now();
    }
}