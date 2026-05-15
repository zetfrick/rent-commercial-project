package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_bans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long adminId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime bannedUntil;

    @Column(nullable = false)
    private LocalDateTime bannedAt = LocalDateTime.now();

    private boolean active = true;

    public UserBan(Long userId, Long adminId, String reason, LocalDateTime bannedUntil) {
        this.userId = userId;
        this.adminId = adminId;
        this.reason = reason;
        this.bannedUntil = bannedUntil;
        this.bannedAt = LocalDateTime.now();
        this.active = true;
    }
}