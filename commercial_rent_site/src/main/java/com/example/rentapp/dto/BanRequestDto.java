package com.example.rentapp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BanRequestDto {
    private Long userId;
    private String reason;
    private String duration; // "1_DAY", "1_WEEK", "1_MONTH", "1_YEAR", "CUSTOM"
    private LocalDateTime customDate;
}