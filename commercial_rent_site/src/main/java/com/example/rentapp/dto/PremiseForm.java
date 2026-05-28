package com.example.rentapp.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PremiseForm {
    private String type;
    private Integer area;
    private Integer capacity;
    private List<String> amenities;

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

    // Контакты
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerMiddleName;
    private String ownerPhone;
    private String ownerEmail;
}