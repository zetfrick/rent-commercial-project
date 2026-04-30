package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private String type;      // "PREMISE" или "USER"

    private Long premiseId;

    private Long userId;

    @Column(nullable = false)
    private Long complainantId;

    @Column(nullable = false)
    private String complainantName;

    @Column(nullable = false, length = 500)
    private String targetName;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        if (status == null || status.isEmpty()) {
            status = "ACTIVE";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private Long resolvedBy;

    private String resolvedByName;

    private LocalDateTime resolvedAt;

    @ElementCollection
    private List<RejectedBy> rejectedBy = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedBy {
        private Long adminId;
        private String adminName;
        private LocalDateTime rejectedAt;
    }

    // Конструктор для жалобы на помещение
    public Complaint(String subject, String reason, Long premiseId, Long complainantId, String complainantName, String targetName) {
        this.subject = subject;
        this.reason = reason;
        this.type = "PREMISE";
        this.premiseId = premiseId;
        this.complainantId = complainantId;
        this.complainantName = complainantName;
        this.targetName = targetName;
        this.status = "ACTIVE";
    }

    // Конструктор для жалобы на пользователя
    public Complaint(String subject, String reason, Long userId, Long complainantId, String complainantName, String targetName, boolean isUser) {
        this.subject = subject;
        this.reason = reason;
        this.type = "USER";
        this.userId = userId;
        this.complainantId = complainantId;
        this.complainantName = complainantName;
        this.targetName = targetName;
        this.status = "ACTIVE";
    }
}