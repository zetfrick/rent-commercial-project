package com.example.rentapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

// @EnableEurekaClient больше НЕ НУЖЕН в Spring Cloud 2023+
@SpringBootApplication
@EnableFeignClients  // Оставляем только это для Feign-клиентов
@EnableScheduling
@EnableAsync// Добавлено для работы Scheduled задач (очистка истекших блокировок)
public class RentAppApplication {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(RentAppApplication.class, args);
    }
}