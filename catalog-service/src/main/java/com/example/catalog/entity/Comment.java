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

    @Column(length = 1000)
    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Comment(Long premiseId, String authorName, String text) {
        this.premiseId = premiseId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}