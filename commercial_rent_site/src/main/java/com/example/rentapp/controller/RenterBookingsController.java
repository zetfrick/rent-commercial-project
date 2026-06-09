package com.example.rentapp.controller;

import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.BookingService;
import com.example.rentapp.service.CatalogServiceWrapper;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/my-rentals")
public class RenterBookingsController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CatalogServiceWrapper catalogService;

    @GetMapping
    public String myRentals(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(required = false) String city,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User currentUser = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем все APPROVED бронирования арендатора
        List<BookingDto> bookings = bookingService.getApprovedBookingsForRenter(currentUser.getId());

        // Группируем по помещениям и загружаем детали
        List<Map<String, Object>> rentals = new ArrayList<>();
        for (BookingDto booking : bookings) {
            PremiseDto premise = catalogService.getPremiseById(booking.getPremiseId());
            if (premise != null) {
                Map<String, Object> rental = new HashMap<>();
                rental.put("booking", booking);
                rental.put("premise", premise);
                rental.put("owner", userService.findById(booking.getOwnerId()).orElse(null));
                rentals.add(rental);
            }
        }

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("rentals", rentals);
        model.addAttribute("currentUser", currentUser);

        return "future/my-rentals";
    }
}