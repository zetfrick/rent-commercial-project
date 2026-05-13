package com.example.rentapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsDto {

    @NotNull(message = "Поле enabled обязательно")
    private Boolean emailNotificationsEnabled;
}