// catalog-service/src/main/java/com/example/catalog/config/PremiseConfig.java
package com.example.catalog.config;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class PremiseConfig {

    // Все допустимые типы помещений (хранятся только здесь)
    public static final List<String> TYPES = List.of(
            "OFFICE",
            "TRADING",
            "WAREHOUSE",
            "PRODUCTION",
            "HOSPITALITY",
            "UNIVERSAL",
            "COWORKING"
    );

    // Все допустимые удобства (хранятся только здесь)
    public static final List<String> AMENITIES = List.of(
            "CONDITIONER",
            "WI_FI",
            "FURNITURE",
            "PARKING",
            "SECURITY",
            "ELEVATOR",
            "KITCHEN",
            "CONFERENCE",
            "PRINTER",
            "COFFEE_MACHINE"
    );

    // Маппинг для русских названий типов
    public static final Map<String, String> TYPE_RUSSIAN = Map.of(
            "OFFICE", "Офисное",
            "TRADING", "Торговое",
            "WAREHOUSE", "Складское",
            "PRODUCTION", "Производственное",
            "HOSPITALITY", "Гостиничное",
            "UNIVERSAL", "Универсальное",
            "COWORKING", "Коворкинг"
    );

    // Маппинг для русских названий удобств
    public static final Map<String, String> AMENITY_RUSSIAN = Map.of(
            "CONDITIONER", "Кондиционер",
            "WI_FI", "Wi-Fi",
            "FURNITURE", "Мебель",
            "PARKING", "Парковка",
            "SECURITY", "Охрана",
            "ELEVATOR", "Лифт",
            "KITCHEN", "Кухня",
            "CONFERENCE", "Конференц-зал",
            "PRINTER", "Принтер / МФУ",
            "COFFEE_MACHINE", "Кофемашина"
    );

    // Геттеры для использования в контроллерах
    public List<String> getTypes() { return TYPES; }
    public List<String> getAmenities() { return AMENITIES; }
    public Map<String, String> getTypeRussian() { return TYPE_RUSSIAN; }
    public Map<String, String> getAmenityRussian() { return AMENITY_RUSSIAN; }
}