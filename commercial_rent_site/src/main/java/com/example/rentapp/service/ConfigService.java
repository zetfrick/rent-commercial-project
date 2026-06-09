package com.example.rentapp.service;

import com.example.rentapp.dto.PremiseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {

    @Autowired
    private CatalogServiceWrapper catalogService;

    // Кэшируем конфигурацию
    private List<String> types = List.of();
    private Map<String, String> typeRussian = Map.of();
    private List<String> amenities = List.of();
    private Map<String, String> amenityRussian = Map.of();

    // Флаг, была ли успешная загрузка конфигурации
    private boolean configLoaded = false;

    @PostConstruct
    public void loadConfig() {
        refreshConfig();
    }

    @Scheduled(fixedDelay = 30000)  // 30 секунд
    public void scheduledRefreshConfig() {
        // Проверяем только если конфигурация ещё не загружена
        if (!configLoaded) {
            if (catalogService.isServiceAvailable()) {
                refreshConfig();
            }
        }
    }

    public void refreshConfig() {
        try {
            Map<String, Object> config = catalogService.getAllConfig();
            this.types = (List<String>) config.get("types");
            this.typeRussian = (Map<String, String>) config.get("typeRussian");
            this.amenities = (List<String>) config.get("amenities");
            this.amenityRussian = (Map<String, String>) config.get("amenityRussian");

            // ВАЖНО: передаём переводы в PremiseDto
            PremiseDto.initTranslations(this.typeRussian, this.amenityRussian);

            configLoaded = true;  // ← Отмечаем, что конфигурация загружена

            System.out.println("✅ Config loaded successfully from catalog-service");
            System.out.println("Types: " + this.types);
            System.out.println("TypeRussian: " + this.typeRussian);
        } catch (Exception e) {
            configLoaded = false;  // ← Загрузка не удалась
            System.err.println("⚠️ Failed to load config from catalog-service: " + e.getMessage());
        }
    }

    public List<String> getTypes() { return types; }
    public Map<String, String> getTypeRussian() { return typeRussian; }
    public List<String> getAmenities() { return amenities; }
    public Map<String, String> getAmenityRussian() { return amenityRussian; }
}