// catalog-service/src/main/java/com/example/catalog/controller/ConfigController.java
package com.example.catalog.controller;

import com.example.catalog.config.PremiseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private PremiseConfig premiseConfig;

    @GetMapping("/types")
    public Map<String, Object> getTypes() {
        return Map.of(
                "types", premiseConfig.getTypes(),
                "russian", premiseConfig.getTypeRussian()
        );
    }

    @GetMapping("/amenities")
    public Map<String, Object> getAmenities() {
        return Map.of(
                "amenities", premiseConfig.getAmenities(),
                "russian", premiseConfig.getAmenityRussian()
        );
    }

    @GetMapping("/all")
    public Map<String, Object> getAllConfig() {
        return Map.of(
                "types", premiseConfig.getTypes(),
                "typeRussian", premiseConfig.getTypeRussian(),
                "amenities", premiseConfig.getAmenities(),
                "amenityRussian", premiseConfig.getAmenityRussian()
        );
    }
}