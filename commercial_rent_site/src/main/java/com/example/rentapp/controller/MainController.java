package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.PremiseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MainController {

    private final CatalogClient catalogClient;

    public MainController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @GetMapping("/")
    public String index(HttpServletRequest request,
                        @RequestParam(required = false) String city,
                        Authentication auth,
                        Model model) {

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/main/landing-logged";
        }
        return "redirect:/main/landing-unlogged";
    }

    @GetMapping("/main/landing-logged")
    public String landingLogged(HttpServletRequest request,
                                @RequestParam(required = false) String city,
                                Model model) {

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        // Загружаем 6 последних добавленных помещений
        try {
            List<PremiseDto> latestPremises = catalogClient.getLatestPremises(6);
            model.addAttribute("latestPremises", latestPremises);
        } catch (Exception e) {
            // Если сервис недоступен — передаём пустой список
            model.addAttribute("latestPremises", List.of());
        }

        return "main/landing-logged";
    }

    @GetMapping("/main/landing-unlogged")
    public String landingUnlogged(HttpServletRequest request,
                                  @RequestParam(required = false) String city,
                                  Model model) {

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        // Загружаем 6 последних добавленных помещений и для неавторизованных тоже
        try {
            List<PremiseDto> latestPremises = catalogClient.getLatestPremises(6);
            model.addAttribute("latestPremises", latestPremises);
        } catch (Exception e) {
            model.addAttribute("latestPremises", List.of());
        }

        return "main/landing-unlogged";
    }

    @GetMapping("/catalog/preview")
    public String catalogPreview(HttpServletRequest request,
                                 @RequestParam(required = false) String city,
                                 Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "catalog/preview";
    }
}