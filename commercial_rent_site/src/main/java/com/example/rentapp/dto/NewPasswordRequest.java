package com.example.rentapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewPasswordRequest {

    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Код обязателен")
    private String code;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 4, message = "Пароль должен содержать минимум 4 символа")
    private String newPassword;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;
}