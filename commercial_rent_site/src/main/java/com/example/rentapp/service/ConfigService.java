package com.example.rentapp.service;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {

    @Autowired
    private CatalogClient catalogClient;

    // Кэшируем конфигурацию
    private List<String> types = List.of();
    private Map<String, String> typeRussian = Map.of();
    private List<String> amenities = List.of();
    private Map<String, String> amenityRussian = Map.of();

    @PostConstruct
    public void loadConfig() {
        refreshConfig();
    }

    public void refreshConfig() {
        try {
            Map<String, Object> config = catalogClient.getAllConfig();
            this.types = (List<String>) config.get("types");
            this.typeRussian = (Map<String, String>) config.get("typeRussian");
            this.amenities = (List<String>) config.get("amenities");
            this.amenityRussian = (Map<String, String>) config.get("amenityRussian");

            // ВАЖНО: передаём переводы в PremiseDto
            PremiseDto.initTranslations(this.typeRussian, this.amenityRussian);

            System.out.println("Config loaded successfully from catalog-service");
            System.out.println("Types: " + this.types);
            System.out.println("TypeRussian: " + this.typeRussian);
            System.out.println("Amenities: " + this.amenities);
            System.out.println("AmenityRussian: " + this.amenityRussian);
        } catch (Exception e) {
            // Fallback на случай недоступности catalog-service
            System.err.println("Failed to load config from catalog-service, using defaults: " + e.getMessage());
            this.types = List.of("OFFICE", "TRADING", "WAREHOUSE", "PRODUCTION", "HOSPITALITY", "UNIVERSAL");
            this.typeRussian = Map.of(
                    "OFFICE", "Офисное",
                    "TRADING", "Торговое",
                    "WAREHOUSE", "Складское",
                    "PRODUCTION", "Производственное",
                    "HOSPITALITY", "Гостиничное",
                    "UNIVERSAL", "Универсальное"
            );
            this.amenities = List.of("CONDITIONER", "WI_FI", "FURNITURE", "PARKING", "SECURITY", "ELEVATOR", "KITCHEN", "CONFERENCE");
            this.amenityRussian = Map.of(
                    "CONDITIONER", "Кондиционер",
                    "WI_FI", "Wi-Fi",
                    "FURNITURE", "Мебель",
                    "PARKING", "Парковка",
                    "SECURITY", "Охрана",
                    "ELEVATOR", "Лифт",
                    "KITCHEN", "Кухня",
                    "CONFERENCE", "Конференц-зал"
            );

            // ВАЖНО: даже при fallback передаём переводы в PremiseDto
            PremiseDto.initTranslations(this.typeRussian, this.amenityRussian);
        }
    }

    public List<String> getTypes() { return types; }
    public Map<String, String> getTypeRussian() { return typeRussian; }
    public List<String> getAmenities() { return amenities; }
    public Map<String, String> getAmenityRussian() { return amenityRussian; }
}