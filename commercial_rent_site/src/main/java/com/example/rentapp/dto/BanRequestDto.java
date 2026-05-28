package com.example.rentapp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BanRequestDto {
    private Long userId;
    private String reason;
    private String duration;
    private LocalDateTime customDate;
}