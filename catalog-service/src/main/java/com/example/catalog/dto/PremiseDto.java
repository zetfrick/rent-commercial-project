package com.example.catalog.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PremiseDto {

    private Long id;
    private Long ownerId;

    private String type;
    private Integer area;
    private Integer capacity;

    private List<String> amenities = new ArrayList<>();

    private Integer priceWeekday;
    private Integer priceWeekend;
    private Integer priceHoliday;

    private LocalDate availableFrom;
    private LocalDate availableTo;

    private String region;
    private String city;
    private String street;
    private String building;
    private String floor;
    private String apartment;

    private String description;
    private String extraFees;
    private String importantInfo;

    private String contactFirstName;
    private String contactLastName;
    private String contactMiddleName;
    private String contactPhone;
    private String contactEmail;

    @Getter
    private Double latitude;
    @Getter
    private Double longitude;

    private List<String> photoPaths = new ArrayList<>();

    private boolean active = true;

    private LocalDate createdAt;

    private LocalDateTime unpublishedAt;

    // Методы перевода УДАЛЕНЫ - они должны быть только в PremiseConfig!
}