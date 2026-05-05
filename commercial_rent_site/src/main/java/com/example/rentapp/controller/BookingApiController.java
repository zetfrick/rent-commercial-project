package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.BookingService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CatalogClient catalogClient;

    // Получить все занятые даты для помещения (только APPROVED)
    @GetMapping("/premise/{premiseId}/booked-dates")
    public ResponseEntity<List<LocalDate>> getBookedDates(@PathVariable Long premiseId) {
        return ResponseEntity.ok(bookingService.getBookedDates(premiseId));
    }

    // Получить все одобренные бронирования для помещения (с деталями арендаторов)
    @GetMapping("/premise/{premiseId}")
    public ResponseEntity<List<BookingDto>> getBookings(@PathVariable Long premiseId) {
        return ResponseEntity.ok(bookingService.getApprovedBookingsWithDetails(premiseId));
    }

    // Получить PENDING запросы для владельца
    @GetMapping("/owner/{ownerId}/pending")
    public ResponseEntity<List<BookingDto>> getPendingRequests(@PathVariable Long ownerId) {
        return ResponseEntity.ok(bookingService.getPendingRequestsForOwner(ownerId));
    }

    // Получить все запросы арендатора (с разными статусами)
    @GetMapping("/renter/{renterId}/requests")
    public ResponseEntity<List<BookingDto>> getRenterRequests(@PathVariable Long renterId) {
        return ResponseEntity.ok(bookingService.getRequestsForRenter(renterId));
    }

    // Создать запрос на аренду (для арендатора)
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> createBookingRequest(
            @RequestParam Long premiseId,
            @RequestParam Long ownerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        if (currentUser.getId().equals(ownerId)) {
            response.put("success", false);
            response.put("message", "Вы не можете арендовать собственное помещение");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            BookingDto booking = bookingService.createBookingRequest(
                    premiseId, currentUser.getId(), ownerId, startDate, endDate
            );
            response.put("success", true);
            response.put("booking", booking);
            response.put("message", "Запрос на аренду отправлен владельцу");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Создать бронирование напрямую (для владельца)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBookingDirect(
            @RequestParam Long premiseId,
            @RequestParam Long renterId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        // Получаем информацию о помещении, чтобы проверить владельца
        PremiseDto premise = catalogClient.getPremiseById(premiseId);
        if (premise == null) {
            response.put("success", false);
            response.put("message", "Помещение не найдено");
            return ResponseEntity.status(404).body(response);
        }

        // Проверяем, что текущий пользователь - владелец помещения
        if (!currentUser.getId().equals(premise.getOwnerId())) {
            response.put("success", false);
            response.put("message", "Только владелец может создать бронирование");
            return ResponseEntity.status(403).body(response);
        }

        // Проверяем, что арендатор существует
        User renter = userService.findById(renterId).orElse(null);
        if (renter == null) {
            response.put("success", false);
            response.put("message", "Арендатор не найден");
            return ResponseEntity.status(404).body(response);
        }

        try {
            BookingDto booking = bookingService.createBookingDirect(
                    premiseId, renterId, premise.getOwnerId(), startDate, endDate
            );
            response.put("success", true);
            response.put("booking", booking);
            response.put("message", "Аренда оформлена!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Одобрить запрос (владелец)
    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<Map<String, Object>> approveBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        try {
            bookingService.approveBooking(bookingId, currentUser.getId());
            response.put("success", true);
            response.put("message", "Запрос одобрен, даты забронированы");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Отказать в запросе (владелец)
    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<Map<String, Object>> rejectBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        try {
            bookingService.rejectBooking(bookingId, currentUser.getId());
            response.put("success", true);
            response.put("message", "Запрос отклонён");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Отменить бронирование (владелец) - ИСПРАВЛЕНО: @DeleteMapping
    @DeleteMapping("/{bookingId}/cancel-by-owner")
    public ResponseEntity<Map<String, Object>> cancelByOwner(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        try {
            bookingService.cancelBookingByOwner(bookingId, currentUser.getId());
            response.put("success", true);
            response.put("message", "Бронирование отменено");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Отменить бронирование (арендатор) - ИСПРАВЛЕНО: @DeleteMapping
    @DeleteMapping("/{bookingId}/cancel-by-renter")
    public ResponseEntity<Map<String, Object>> cancelByRenter(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(404).body(response);
        }

        try {
            bookingService.cancelBookingByRenter(bookingId, currentUser.getId());
            response.put("success", true);
            response.put("message", "Бронирование отменено");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Проверить доступность дат (только для одобренных бронирований)
    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Long premiseId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Map<String, Object> response = new HashMap<>();
        boolean available = bookingService.areDatesAvailable(premiseId, startDate, endDate);
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/premise/{premiseId}/booked-ranges")
    public ResponseEntity<List<Map<String, Object>>> getBookedDateRanges(@PathVariable Long premiseId) {
        return ResponseEntity.ok(bookingService.getBookedDateRanges(premiseId));
    }

    // Проверить доступность дат с учётом периода доступности помещения
    @GetMapping("/check-availability-with-period")
    public ResponseEntity<Map<String, Object>> checkAvailabilityWithPeriod(
            @RequestParam Long premiseId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Map<String, Object> response = new HashMap<>();

        // Получаем информацию о помещении
        PremiseDto premise = catalogClient.getPremiseById(premiseId);
        if (premise == null) {
            response.put("available", false);
            response.put("message", "Помещение не найдено");
            return ResponseEntity.badRequest().body(response);
        }

        // Проверяем границы периода
        if (startDate.isBefore(premise.getAvailableFrom()) || endDate.isAfter(premise.getAvailableTo())) {
            response.put("available", false);
            response.put("message", "Выбранные даты выходят за пределы доступного периода аренды");
            response.put("availableFrom", premise.getAvailableFrom());
            response.put("availableTo", premise.getAvailableTo());
            return ResponseEntity.ok(response);
        }

        boolean available = bookingService.areDatesAvailable(premiseId, startDate, endDate);
        response.put("available", available);
        if (!available) {
            response.put("message", "Выбранные даты уже заняты");
        }
        return ResponseEntity.ok(response);
    }
}