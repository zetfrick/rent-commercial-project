// rentapp/src/main/java/com/example/rentapp/advice/GlobalModelAdvice.java
package com.example.rentapp.advice;

import com.example.rentapp.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private ConfigService configService;

    @ModelAttribute("types")
    public java.util.List<String> getTypes() {
        return configService.getTypes();
    }

    @ModelAttribute("amenities")
    public java.util.List<String> getAmenities() {
        return configService.getAmenities();
    }

    @ModelAttribute("typeInRussian")
    public java.util.Map<String, String> getTypeRussian() {
        return configService.getTypeRussian();
    }

    @ModelAttribute("amenityInRussian")
    public java.util.Map<String, String> getAmenityRussian() {
        return configService.getAmenityRussian();
    }
}