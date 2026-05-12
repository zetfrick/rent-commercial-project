package com.example.rentapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeRequest {

    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Код обязателен")
    private String code;
}