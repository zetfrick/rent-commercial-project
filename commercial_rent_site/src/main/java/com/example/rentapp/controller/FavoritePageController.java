package com.example.rentapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class FavoritePageController {

    @GetMapping("/favorites")
    public String favoritesPage() {
        return "future/favorites";
    }
}