package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class WebCatalogController {

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private CommentService commentService;

    @GetMapping("/catalog")
    public String catalog(Model model) {
        List<PremiseDto> premises = catalogClient.getAllPremises();
        model.addAttribute("premises", premises);

        model.addAttribute("types", List.of("OFFICE", "TRADING", "WAREHOUSE", "PRODUCTION", "HOSPITALITY", "UNIVERSAL"));
        model.addAttribute("amenities", List.of("CONDITIONER", "WI_FI", "FURNITURE", "PARKING", "SECURITY", "ELEVATOR", "KITCHEN", "CONFERENCE"));

        model.addAttribute("typeInRussian", Map.of(
                "OFFICE", "Офисное", "TRADING", "Торговое", "WAREHOUSE", "Складское",
                "PRODUCTION", "Производственное", "HOSPITALITY", "Гостиничное", "UNIVERSAL", "Универсальное"
        ));

        model.addAttribute("amenityInRussian", Map.of(
                "CONDITIONER", "Кондиционер", "WI_FI", "Wi-Fi", "FURNITURE", "Мебель",
                "PARKING", "Парковка", "SECURITY", "Охрана", "ELEVATOR", "Лифт",
                "KITCHEN", "Кухня", "CONFERENCE", "Конференц-зал"
        ));

        return "future/catalog";
    }

    @GetMapping("/premise/{id}")
    public String premiseDetail(@PathVariable Long id, Model model) {
        PremiseDto premise = catalogClient.getPremiseById(id);

        if (premise == null) {
            return "redirect:/catalog";
        }

        model.addAttribute("premise", premise);
        return "future/premise-detail";
    }

    // ==================== REST ЭНДПОИНТЫ ДЛЯ КОММЕНТАРИЕВ ====================

    @PostMapping("/api/comments")
    @ResponseBody
    public CommentDto addComment(@RequestBody CommentDto commentDto,
                                 @AuthenticationPrincipal UserDetails userDetails) {  // ← ДОБАВЛЕН ПАРАМЕТР
        // Устанавливаем имя автора из текущего авторизованного пользователя
        if (userDetails != null) {
            commentDto.setAuthorName(userDetails.getUsername());
        }
        return commentService.addComment(commentDto);
    }

    @GetMapping("/api/comments/premise/{premiseId}")
    @ResponseBody
    public List<CommentDto> getComments(@PathVariable Long premiseId) {
        return commentService.getCommentsByPremiseId(premiseId);
    }
}