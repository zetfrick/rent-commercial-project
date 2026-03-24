package com.example.rentapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(HttpServletRequest request, @RequestParam(required = false) String city, Authentication auth, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/main/landing-logged";
        }
        return "redirect:/main/landing-unlogged";
    }

    @GetMapping("/main/landing-logged")
    public String landingLogged(HttpServletRequest request, @RequestParam(required = false) String city, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "main/landing-logged";
    }

    @GetMapping("/main/landing-unlogged")
    public String landingUnlogged(HttpServletRequest request, @RequestParam(required = false) String city, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "main/landing-unlogged";
    }

    @GetMapping("/catalog/preview")
    public String catalogPreview(HttpServletRequest request, @RequestParam(required = false) String city, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "catalog/preview";
    }
}