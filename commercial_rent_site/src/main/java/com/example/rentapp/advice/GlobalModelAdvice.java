package com.example.rentapp.advice;

import com.example.rentapp.entity.User;
import com.example.rentapp.service.ConfigService;
import com.example.rentapp.service.NotificationService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private NotificationService notificationService;

    @ModelAttribute("types")
    public java.util.List<String> getTypes() {
        return configService.getTypes();
    }

    @ModelAttribute("amenities")
    public java.util.List<String> getAmenities() {
        return configService.getAmenities();
    }

    @ModelAttribute("typeInRussian")
    public java.util.Map<String, String> getTypeRussian() {
        return configService.getTypeRussian();
    }

    @ModelAttribute("amenityInRussian")
    public java.util.Map<String, String> getAmenityRussian() {
        return configService.getAmenityRussian();
    }

    @ModelAttribute("unreadNotificationsCount")
    public int getUnreadNotificationsCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                User user = userService.findByLogin(auth.getName()).orElse(null);
                if (user != null && notificationService != null) {
                    return notificationService.getUnreadNotificationsCount(user.getId());
                }
            } catch (Exception e) {
                System.err.println("Error getting unread notifications count: " + e.getMessage());
            }
        }
        return 0;
    }
}