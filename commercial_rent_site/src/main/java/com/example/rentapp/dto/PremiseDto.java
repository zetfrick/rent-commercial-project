package com.example.rentapp.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PremiseDto {

    private Long id;
    private Long ownerId;  // вместо User

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

    // getters + setters
    @Getter
    private Double latitude;
    @Getter
    private Double longitude;

    private List<String> photoPaths = new ArrayList<>();

    private List<CommentDto> comments = new ArrayList<>();

    private boolean active = true;

    private LocalDate createdAt;

    // Методы перевода (копируй из catalog-service)
    public String getTypeInRussian() {
        if (type == null) return "";
        return switch (type) {
            case "OFFICE" -> "Офисное";
            case "TRADING" -> "Торговое";
            case "WAREHOUSE" -> "Складское";
            case "PRODUCTION" -> "Производственное";
            case "HOSPITALITY" -> "Гостиничное";
            case "UNIVERSAL" -> "Универсальное";
            default -> type;
        };
    }

    public String getAmenityInRussian(String english) {
        if (english == null) return "";
        return switch (english) {
            case "CONDITIONER" -> "Кондиционер";
            case "WI_FI" -> "Wi-Fi";
            case "FURNITURE" -> "Мебель";
            case "PARKING" -> "Парковка";
            case "SECURITY" -> "Охрана";
            case "ELEVATOR" -> "Лифт";
            case "KITCHEN" -> "Кухня";
            case "CONFERENCE" -> "Конференц-зал";
            default -> english;
        };
    }
}