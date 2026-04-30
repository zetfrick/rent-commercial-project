package com.example.rentapp.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private List<CommentDto> comments = new ArrayList<>();

    private boolean active = true;

    private LocalDate createdAt;

    // НОВОЕ ПОЛЕ: дата снятия с публикации
    private LocalDateTime unpublishedAt;

    // Статические карты для переводов (будут заполняться из ConfigService)
    private static Map<String, String> typeRussianMap = Map.of();
    private static Map<String, String> amenityRussianMap = Map.of();

    /**
     * Инициализация переводов (вызывается из ConfigService при загрузке)
     */
    public static void initTranslations(Map<String, String> typeRussian, Map<String, String> amenityRussian) {
        if (typeRussian != null && !typeRussian.isEmpty()) {
            typeRussianMap = typeRussian;
        }
        if (amenityRussian != null && !amenityRussian.isEmpty()) {
            amenityRussianMap = amenityRussian;
        }
    }

    public String getTypeInRussian() {
        if (type == null) return "";
        return typeRussianMap.getOrDefault(type, type);
    }

    public String getAmenityInRussian(String amenity) {
        if (amenity == null) return "";
        return amenityRussianMap.getOrDefault(amenity, amenity);
    }

    // Метод для получения даты удаления (через 60 дней после снятия)
    public LocalDate getDeletionDate() {
        if (unpublishedAt == null) return null;
        return unpublishedAt.plusDays(60).toLocalDate();
    }

    // Метод для получения количества дней до удаления
    public long getDaysUntilDeletion() {
        if (unpublishedAt == null) return 0;
        LocalDateTime deletionDate = unpublishedAt.plusDays(60);
        LocalDateTime now = LocalDateTime.now();
        if (deletionDate.isBefore(now)) return 0;
        return java.time.Duration.between(now, deletionDate).toDays();
    }
}