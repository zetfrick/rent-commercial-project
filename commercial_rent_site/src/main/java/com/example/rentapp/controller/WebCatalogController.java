package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.CommentService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class WebCatalogController {

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    // УПРОЩЁННО: types, amenities, typeInRussian, amenityInRussian добавляются автоматически через GlobalModelAdvice
    @GetMapping("/catalog")
    public String catalog(Model model) {
        List<PremiseDto> premises = catalogClient.getAllPremises();
        model.addAttribute("premises", premises);
        return "future/catalog";
    }

    @GetMapping("/premise/{id}")
    public String premiseDetail(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                jakarta.servlet.http.HttpServletRequest request,
                                @RequestParam(required = false) String city,
                                Model model) {
        PremiseDto premise = catalogClient.getPremiseById(id);

        if (premise == null) {
            return "redirect:/catalog";
        }

        // Для header
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        // Проверяем, является ли текущий пользователь владельцем помещения (только если пользователь авторизован)
        boolean isOwner = false;
        String ownerLogin = null;

        if (userDetails != null) {
            User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
            if (currentUser != null && premise.getOwnerId() != null) {
                isOwner = currentUser.getId().equals(premise.getOwnerId());
            }
        }

        // Получаем логин владельца по ownerId
        if (premise.getOwnerId() != null) {
            Optional<User> owner = userService.findById(premise.getOwnerId());
            if (owner.isPresent()) {
                ownerLogin = owner.get().getLogin();
            }
        }

        model.addAttribute("premise", premise);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("ownerLogin", ownerLogin);

        // types, amenities и прочее уже в модели через GlobalModelAdvice

        return "future/premise-detail";
    }

    // НОВЫЙ МЕТОД: страница редактирования помещения
    @GetMapping("/premise/{id}/edit")
    public String editPremiseForm(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  jakarta.servlet.http.HttpServletRequest request,
                                  @RequestParam(required = false) String city,
                                  Model model) {
        PremiseDto premise = catalogClient.getPremiseById(id);

        if (premise == null) {
            return "redirect:/catalog";
        }

        // Для header
        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        // Проверяем, является ли текущий пользователь владельцем
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        if (!currentUser.getId().equals(premise.getOwnerId())) {
            return "redirect:/premise/" + id + "?error=access_denied";
        }

        model.addAttribute("premise", premise);
        // types, amenities и прочее уже в модели через GlobalModelAdvice

        return "future/premise-edit";
    }

    // НОВЫЙ МЕТОД: сохранение изменений помещения
    @PostMapping("/premise/{id}/edit")
    public String updatePremise(@PathVariable Long id,
                                @RequestParam String type,
                                @RequestParam Integer area,
                                @RequestParam Integer capacity,
                                @RequestParam(required = false) List<String> amenities,
                                @RequestParam String description,
                                @RequestParam(required = false) String extraFees,
                                @RequestParam(required = false) String importantInfo,
                                @AuthenticationPrincipal UserDetails userDetails) {

        PremiseDto premise = catalogClient.getPremiseById(id);

        if (premise == null) {
            return "redirect:/catalog";
        }

        // Проверяем, является ли текущий пользователь владельцем
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        if (!currentUser.getId().equals(premise.getOwnerId())) {
            return "redirect:/premise/" + id + "?error=access_denied";
        }

        // Обновляем только разрешённые поля
        premise.setType(type);
        premise.setArea(area);
        premise.setCapacity(capacity);
        premise.setAmenities(amenities != null ? amenities : List.of());
        premise.setDescription(description);
        premise.setExtraFees(extraFees);
        premise.setImportantInfo(importantInfo);

        // Отправляем обновление через Feign клиент
        catalogClient.updatePremise(id, premise);

        return "redirect:/premise/" + id + "?updated=true";
    }

    // ==================== REST ЭНДПОИНТЫ ДЛЯ КОММЕНТАРИЕВ ====================

    @PostMapping("/api/comments")
    @ResponseBody
    public CommentDto addComment(@RequestBody CommentDto commentDto,
                                 @AuthenticationPrincipal UserDetails userDetails) {
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