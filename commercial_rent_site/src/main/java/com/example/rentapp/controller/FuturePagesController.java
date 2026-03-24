package com.example.rentapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FuturePagesController {

    @GetMapping("/contacts")
    public String contacts(HttpServletRequest request, @RequestParam(required = false) String city, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "future/contacts";  // Без "templates/"
    }

    @GetMapping("/about")
    public String about(HttpServletRequest request, @RequestParam(required = false) String city, Model model) {
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        return "future/about";  // Без "templates/"
    }
}