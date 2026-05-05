package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long premiseId;

    @Column(nullable = false)
    private Long renterId;      // ID арендатора (кто снимает)

    @Column(nullable = false)
    private Long ownerId;       // ID владельца помещения

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String renterName;   // логин арендатора

    @Column(nullable = false)
    private String ownerName;    // логин владельца

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Статусы: PENDING (ожидает), APPROVED (одобрена), REJECTED (отказано), CANCELLED (отменено)
    private String status = "PENDING";

    // Конструктор для создания запроса на аренду
    public Booking(Long premiseId, Long renterId, Long ownerId, LocalDate startDate, LocalDate endDate,
                   String renterName, String ownerName) {
        this.premiseId = premiseId;
        this.renterId = renterId;
        this.ownerId = ownerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.renterName = renterName;
        this.ownerName = ownerName;
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }
}