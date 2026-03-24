package com.example.rentapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// @EnableEurekaClient больше НЕ НУЖЕН в Spring Cloud 2023+
@SpringBootApplication
@EnableFeignClients  // Оставляем только это для Feign-клиентов
public class RentAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentAppApplication.class, args);
    }
}