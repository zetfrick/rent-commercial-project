package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class WebCatalogController {

    @Autowired
    private CatalogClient catalogClient;

    @GetMapping("/catalog")
    public String catalog(Model model) {
        List<PremiseDto> premises = catalogClient.getAllPremises();
        model.addAttribute("premises", premises);
        return "future/catalog";  // или твой шаблон catalog.html
    }

    @GetMapping("/premise/{id}")
    public String premiseDetail(@PathVariable Long id, Model model) {
        PremiseDto premise = catalogClient.getPremiseById(id);
        model.addAttribute("premise", premise);
        return "future/premise-detail";
    }
}