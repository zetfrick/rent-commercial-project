package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "premise_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long premiseId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Favorite(Long userId, Long premiseId) {
        this.userId = userId;
        this.premiseId = premiseId;
        this.createdAt = LocalDateTime.now();
    }
}