package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@Controller
public class WebCatalogController {

    @Autowired
    private CatalogClient catalogClient;

    @GetMapping("/catalog")
    public String catalog(Model model) {
        List<PremiseDto> premises = catalogClient.getAllPremises();
        model.addAttribute("premises", premises);

        // Передаём списки для фильтров
        model.addAttribute("types", List.of("OFFICE", "TRADING", "WAREHOUSE", "PRODUCTION", "HOSPITALITY", "UNIVERSAL"));

        model.addAttribute("amenities", List.of("CONDITIONER", "WI_FI", "FURNITURE", "PARKING",
                "SECURITY", "ELEVATOR", "KITCHEN", "CONFERENCE"));

        // Для красивого отображения названий на русском
        model.addAttribute("typeInRussian", Map.of(
                "OFFICE", "Офисное",
                "TRADING", "Торговое",
                "WAREHOUSE", "Складское",
                "PRODUCTION", "Производственное",
                "HOSPITALITY", "Гостиничное",
                "UNIVERSAL", "Универсальное"
        ));

        model.addAttribute("amenityInRussian", Map.of(
                "CONDITIONER", "Кондиционер",
                "WI_FI", "Wi-Fi",
                "FURNITURE", "Мебель",
                "PARKING", "Парковка",
                "SECURITY", "Охрана",
                "ELEVATOR", "Лифт",
                "KITCHEN", "Кухня",
                "CONFERENCE", "Конференц-зал"
        ));

        return "future/catalog";   // или "future/catalog" — в зависимости от вашего пути
    }

    @GetMapping("/premise/{id}")
    public String premiseDetail(@PathVariable Long id, Model model) {
        PremiseDto premise = catalogClient.getPremiseById(id);
        model.addAttribute("premise", premise);
        return "future/premise-detail";
    }
}