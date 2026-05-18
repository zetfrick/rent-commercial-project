package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.entity.UserBan;
import com.example.rentapp.service.BookingService;
import com.example.rentapp.service.CommentService;
import com.example.rentapp.service.NotificationService;
import com.example.rentapp.service.UserBanService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WebCatalogController {

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserBanService userBanService;

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
        boolean isBanned = false;
        UserBan activeBan = null;

        if (userDetails != null) {
            User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
            if (currentUser != null && premise.getOwnerId() != null) {
                isOwner = currentUser.getId().equals(premise.getOwnerId());
            }

            // Проверяем блокировку текущего пользователя
            if (currentUser != null) {
                isBanned = userBanService.isUserBanned(currentUser.getId());
                if (isBanned) {
                    activeBan = userBanService.getActiveBan(currentUser.getId()).orElse(null);
                }
            }
        }

        // Получаем логин владельца по ownerId
        if (premise.getOwnerId() != null) {
            Optional<User> owner = userService.findById(premise.getOwnerId());
            if (owner.isPresent()) {
                ownerLogin = owner.get().getLogin();
            }
        }

        // Получаем занятые даты (только APPROVED) - для проверки доступности и отображения обычным пользователям
        List<LocalDate> bookedDates = bookingService.getBookedDates(id);

        // Получаем диапазоны занятых дат для компактного отображения (только APPROVED)
        List<Map<String, Object>> bookedDateRanges = bookingService.getBookedDateRanges(id);

        // Получаем диапазоны ожидающих дат для компактного отображения (только PENDING)
        List<Map<String, Object>> pendingDateRanges = bookingService.getPendingDateRanges(id);

        // Получаем ожидающие запросы с деталями (с информацией об арендаторе)
        List<Map<String, Object>> pendingRequestsWithDetails = bookingService.getPendingRequestsWithDetails(id);

        // Получаем ВСЕ бронирования
        List<BookingDto> allBookings = bookingService.getBookingsWithDetails(id);

        // Разделяем бронирования на APPROVED и PENDING
        List<BookingDto> approvedBookings = allBookings.stream()
                .filter(b -> "APPROVED".equals(b.getStatus()))
                .collect(Collectors.toList());

        List<BookingDto> pendingBookings = allBookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .collect(Collectors.toList());

        // Устанавливаем данные в DTO
        premise.setBookedDates(bookedDates);
        premise.setBookings(approvedBookings);

        // Добавляем атрибуты в модель
        model.addAttribute("premise", premise);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("ownerLogin", ownerLogin);
        model.addAttribute("isBanned", isBanned);
        model.addAttribute("banInfo", activeBan);
        model.addAttribute("isUserBanned", isBanned);
        model.addAttribute("bookedDates", bookedDates);                          // для всех пользователей
        model.addAttribute("bookedDateRanges", bookedDateRanges);                // для компактного отображения (только APPROVED)
        model.addAttribute("pendingDateRanges", pendingDateRanges);              // диапазоны ожидающих запросов
        model.addAttribute("pendingRequestsWithDetails", pendingRequestsWithDetails); // детали ожидающих запросов (с арендаторами)
        model.addAttribute("approvedBookings", isOwner ? approvedBookings : List.of()); // подтверждённые бронирования
        model.addAttribute("pendingBookings", isOwner ? pendingBookings : List.of());   // ожидающие бронирования

        // Проверяем, можно ли опубликовать объявление снова (дата окончания не прошла)
        boolean canRepublish = false;
        if (premise != null && premise.getAvailableTo() != null) {
            LocalDate today = LocalDate.now();
            // Можно опубликовать, если сегодняшняя дата не позже даты окончания
            canRepublish = !today.isAfter(premise.getAvailableTo());
        }
        model.addAttribute("canRepublish", canRepublish);

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

        // Проверяем, заблокирован ли пользователь
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        if (userBanService.isUserBanned(currentUser.getId())) {
            return "redirect:/premise/" + id + "?error=banned";
        }

        // Проверяем, является ли текущий пользователь владельцем
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

        // Проверяем, заблокирован ли пользователь
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        if (userBanService.isUserBanned(currentUser.getId())) {
            return "redirect:/premise/" + id + "?error=banned";
        }

        // Проверяем, является ли текущий пользователь владельцем
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
            // Проверяем, не заблокирован ли пользователь
            User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
            if (currentUser != null && userBanService.isUserBanned(currentUser.getId())) {
                throw new RuntimeException("Ваш аккаунт заблокирован. Вы не можете оставлять комментарии.");
            }
            commentDto.setAuthorName(userDetails.getUsername());
        }

        CommentDto saved = commentService.addComment(commentDto);

        // ===== СОЗДАЁМ УВЕДОМЛЕНИЕ ВЛАДЕЛЬЦУ ПОМЕЩЕНИЯ =====
        try {
            // Получаем информацию о помещении, чтобы узнать владельца
            PremiseDto premise = catalogClient.getPremiseById(commentDto.getPremiseId());

            if (premise != null && userDetails != null) {
                User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);

                // Отправляем уведомление владельцу, если комментатор не является владельцем
                if (currentUser != null && !currentUser.getId().equals(premise.getOwnerId())) {
                    notificationService.createNotification(
                            premise.getOwnerId(),                    // кому (владельцу помещения)
                            "COMMENT",                               // тип
                            saved.getId(),                           // ID комментария
                            currentUser.getId(),                     // от кого (комментатор)
                            currentUser.getLogin(),                  // имя комментатора
                            commentDto.getText() != null ? commentDto.getText().substring(0, Math.min(commentDto.getText().length(), 100)) : "",  // текст комментария (первые 100 символов)
                            "/premise/" + commentDto.getPremiseId()   // ссылка на объявление
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification for comment: " + e.getMessage());
            // Не прерываем выполнение, если уведомление не отправилось
        }

        return saved;
    }

    @GetMapping("/api/comments/premise/{premiseId}")
    @ResponseBody
    public List<CommentDto> getComments(@PathVariable Long premiseId) {
        return commentService.getCommentsByPremiseId(premiseId);
    }
}