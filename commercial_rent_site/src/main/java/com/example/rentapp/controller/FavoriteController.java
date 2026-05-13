package com.example.rentapp.controller;

import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.service.FavoriteService;
import com.example.rentapp.service.UserService;
import com.example.rentapp.client.CatalogClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final CatalogClient catalogClient;

    @PostMapping("/toggle/{premiseId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long premiseId) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        Long userId = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"))
                .getId();

        boolean isAdded = favoriteService.toggleFavorite(userId, premiseId);

        response.put("success", true);
        response.put("isFavorite", isAdded);
        response.put("message", isAdded ? "Добавлено в избранное" : "Удалено из избранного");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{premiseId}")
    public ResponseEntity<Map<String, Object>> checkFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long premiseId) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("isFavorite", false);
            return ResponseEntity.ok(response);
        }

        Long userId = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"))
                .getId();

        response.put("isFavorite", favoriteService.isFavorite(userId, premiseId));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PremiseDto>> getFavoritePremises(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"))
                .getId();

        List<Long> favoriteIds = favoriteService.getFavoritePremiseIds(userId);

        List<PremiseDto> favoritePremises = favoriteIds.stream()
                .map(catalogClient::getPremiseById)
                .filter(p -> p != null)
                .collect(Collectors.toList());

        return ResponseEntity.ok(favoritePremises);
    }
}