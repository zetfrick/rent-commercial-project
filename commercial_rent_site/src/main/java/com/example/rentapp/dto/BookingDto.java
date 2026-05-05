package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    private Long premiseId;
    private Long renterId;
    private Long ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String renterName;
    private String ownerName;
    private LocalDateTime createdAt;
    private String status;
}