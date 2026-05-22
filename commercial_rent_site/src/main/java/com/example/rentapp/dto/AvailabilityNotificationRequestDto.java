package com.example.rentapp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AvailabilityNotificationRequestDto {
    private Long premiseId;
    private LocalDate startDate;
    private LocalDate endDate;
}