// catalog-service/src/main/java/com/example/catalog/entity/Comment.java
package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long premiseId;

    private String authorName;

    private Long authorId;  // НОВОЕ ПОЛЕ

    @Column(length = 1000)
    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Long parentCommentId;
    private Long repliedToUserId;
    private String repliedToUserName;

    public Comment(Long premiseId, String authorName, String text) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public Comment(Long premiseId, String authorName, String text,
                   Long parentCommentId, Long repliedToUserId, String repliedToUserName) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.parentCommentId = parentCommentId;
        this.repliedToUserId = repliedToUserId;
        this.repliedToUserName = repliedToUserName;
        this.createdAt = LocalDateTime.now();
    }

    // Конструктор с authorId
    public Comment(Long premiseId, Long authorId, String authorName, String text,
                   Long parentCommentId, Long repliedToUserId, String repliedToUserName) {
        this.premiseId = premiseId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.parentCommentId = parentCommentId;
        this.repliedToUserId = repliedToUserId;
        this.repliedToUserName = repliedToUserName;
        this.createdAt = LocalDateTime.now();
    }
}