package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long premiseId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean notified = false;

    public AvailabilityNotification(Long userId, Long premiseId, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.premiseId = premiseId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
        this.notified = false;
    }
}