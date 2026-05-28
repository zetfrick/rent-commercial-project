package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "premises")
@Data
public class Premise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private String type;
    private Integer area;
    private Integer capacity;

    @ElementCollection
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

    private Double latitude;
    private Double longitude;

    @ElementCollection
    private List<String> photoPaths = new ArrayList<>();

    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt = LocalDate.now();

    private LocalDateTime unpublishedAt;

}