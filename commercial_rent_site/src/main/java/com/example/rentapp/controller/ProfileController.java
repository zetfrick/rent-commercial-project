package com.example.rentapp.controller;

import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.dto.PremiseForm;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CatalogServiceWrapper catalogService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserBanService userBanService;

    // ==================== ПРОФИЛЬ ====================

    @GetMapping("/profile")
    public String profile(@RequestParam(required = false) String username,
                          @AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(required = false) String city,
                          HttpServletRequest request,
                          Model model) {

        String currentUsername = (userDetails != null) ? userDetails.getUsername() : null;

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        String targetUsername;
        if (username != null && !username.trim().isEmpty()) {
            targetUsername = username.trim();
        } else {
            targetUsername = currentUsername;
        }

        if (targetUsername == null) {
            return "redirect:/auth/login";
        }

        User profileUser = userService.findByLogin(targetUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + targetUsername));

        User currentUser = (currentUsername != null)
                ? userService.findByLogin(currentUsername).orElse(null)
                : null;

        // ========== СТАТИСТИКА ДЛЯ ПРОФИЛЯ ==========

        // 1. Количество активных объявлений (Сдаётся)
        List<PremiseDto> userPremises = catalogService.getPremisesByOwnerId(profileUser.getId());
        long rentedOutCount = userPremises.stream()
                .filter(PremiseDto::isActive)
                .count();

        // 2. Количество арендованных помещений (Арендовано)
        List<BookingDto> renterBookings = bookingService.getApprovedBookingsForRenter(profileUser.getId());
        long rentedInCount = renterBookings.size();

        // 3. Количество дней с нами
        long daysWithUs = 0;
        if (profileUser.getCreatedAt() != null) {
            daysWithUs = java.time.temporal.ChronoUnit.DAYS.between(
                    profileUser.getCreatedAt(),
                    LocalDateTime.now()
            );
        }

        model.addAttribute("rentedOutCount", rentedOutCount);
        model.addAttribute("rentedInCount", rentedInCount);
        model.addAttribute("daysWithUs", daysWithUs);

        // Если это свой профиль и данные в сессии устарели, обновляем сессию
        if (targetUsername.equals(currentUsername) && currentUser != null && userDetails != null) {
            boolean needsUpdate = false;
            if (userDetails instanceof org.springframework.security.core.userdetails.User) {
                if (!profileUser.getEmail().equals(currentUser.getEmail())) {
                    needsUpdate = true;
                }
            }

            if (needsUpdate) {
                org.springframework.security.core.userdetails.User updatedUserDetails =
                        new org.springframework.security.core.userdetails.User(
                                profileUser.getLogin(),
                                profileUser.getPassword(),
                                userDetails.getAuthorities()
                        );

                UsernamePasswordAuthenticationToken newAuth =
                        new UsernamePasswordAuthenticationToken(
                                updatedUserDetails,
                                userDetails.getPassword(),
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(newAuth);
                request.getSession().setAttribute(
                        SecurityContextHolder.class.getName(),
                        SecurityContextHolder.getContext()
                );
            }
        }

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", targetUsername.equals(currentUsername));
        model.addAttribute("isUserBanned", userBanService.isUserBanned(profileUser.getId()));

        model.addAttribute("editMode", false);
        return "future/profile";
    }

    // ==================== РЕДАКТИРОВАНИЕ ПРОФИЛЯ ====================

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(required = false) String city,
                              HttpServletRequest request,
                              Model model) {
        User user = getCurrentUser(userDetails);

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        model.addAttribute("profileUser", user);
        model.addAttribute("isOwnProfile", true);
        model.addAttribute("editMode", true);
        model.addAttribute("isUserBanned", userBanService.isUserBanned(user.getId()));
        return "future/profile";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute User updatedUser,
                              HttpServletRequest httpRequest) {

        User currentUser = getCurrentUser(userDetails);

        if (!currentUser.getId().equals(updatedUser.getId())) {
            return "redirect:/profile?error=access_denied";
        }

        // Сохраняем старые значения для сравнения
        String oldFirstName = currentUser.getFirstName();
        String oldLastName = currentUser.getLastName();
        String oldMiddleName = currentUser.getMiddleName();
        String oldPhone = currentUser.getPhone();

        // Обновляем профиль
        currentUser.setFirstName(updatedUser.getFirstName());
        currentUser.setLastName(updatedUser.getLastName());
        currentUser.setMiddleName(updatedUser.getMiddleName());
        currentUser.setPhone(updatedUser.getPhone());

        userService.save(currentUser);

        // Проверяем, изменились ли контактные данные
        boolean contactsChanged = false;
        Map<String, String> contacts = new HashMap<>();

        if (!Objects.equals(oldFirstName, currentUser.getFirstName()) && currentUser.getFirstName() != null) {
            contacts.put("firstName", currentUser.getFirstName());
            contactsChanged = true;
        }
        if (!Objects.equals(oldLastName, currentUser.getLastName()) && currentUser.getLastName() != null) {
            contacts.put("lastName", currentUser.getLastName());
            contactsChanged = true;
        }
        if (!Objects.equals(oldMiddleName, currentUser.getMiddleName()) && currentUser.getMiddleName() != null) {
            contacts.put("middleName", currentUser.getMiddleName());
            contactsChanged = true;
        }
        if (!Objects.equals(oldPhone, currentUser.getPhone()) && currentUser.getPhone() != null) {
            contacts.put("phone", currentUser.getPhone());
            contactsChanged = true;
        }

        // Если контакты изменились, обновляем их в catalog-service
        if (contactsChanged) {
            try {
                catalogService.updateOwnerContacts(currentUser.getId(), contacts);
                System.out.println("✅ Контакты владельца #" + currentUser.getId() + " синхронизированы с catalog-service");
            } catch (Exception e) {
                System.err.println("❌ Ошибка синхронизации контактов: " + e.getMessage());
            }
        }

        // ===== ОБНОВЛЯЕМ СЕССИЮ SPRING SECURITY =====
        org.springframework.security.core.userdetails.User updatedUserDetails =
                new org.springframework.security.core.userdetails.User(
                        currentUser.getLogin(),
                        currentUser.getPassword(),
                        userDetails.getAuthorities()
                );

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        updatedUserDetails,
                        userDetails.getPassword(),
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(newAuth);

        httpRequest.getSession().setAttribute(
                SecurityContextHolder.class.getName(),
                SecurityContextHolder.getContext()
        );

        return "redirect:/profile?updated=true";
    }

    // ==================== ДОБАВЛЕНИЕ ПОМЕЩЕНИЯ ====================

    @GetMapping("/premise/add")
    public String addPremiseForm(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam(required = false) String city,
                                 HttpServletRequest request,
                                 Model model) {
        // ===== ПРОВЕРКА ДОСТУПНОСТИ CATALOG-SERVICE =====
        if (!catalogService.isServiceAvailable()) {
            model.addAttribute("error", "Сервис временно недоступен. Пожалуйста, попробуйте позже.");
            model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
            model.addAttribute("currentUri", request.getRequestURI());
            return "error/service-unavailable"; // нужно создать эту страницу
        }

        // Если пользователь не авторизован, перенаправляем на логин
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User owner = getCurrentUser(userDetails);

        if (userBanService.isUserBanned(owner.getId())) {
            model.addAttribute("error", "Ваш аккаунт заблокирован. Вы не можете добавлять помещения.");
            return "redirect:/profile?banned=true";
        }

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        PremiseForm form = new PremiseForm();
        form.setOwnerFirstName(owner.getFirstName());
        form.setOwnerLastName(owner.getLastName());
        form.setOwnerMiddleName(owner.getMiddleName());
        form.setOwnerPhone(owner.getPhone());
        form.setOwnerEmail(owner.getEmail());

        model.addAttribute("premiseForm", form);
        return "future/add-premise";
    }

    @PostMapping("/premise/add")
    public String addPremise(@AuthenticationPrincipal UserDetails userDetails,
                             @ModelAttribute PremiseForm form,
                             @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                             @RequestParam("latitude") String latitudeStr,
                             @RequestParam("longitude") String longitudeStr,
                             Model model) {

        // Если пользователь не авторизован, перенаправляем на логин
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User owner = getCurrentUser(userDetails);

        if (userBanService.isUserBanned(owner.getId())) {
            model.addAttribute("error", "Ваш аккаунт заблокирован. Вы не можете добавлять помещения.");
            return "redirect:/profile?banned=true";
        }

        PremiseDto premiseDto = new PremiseDto();
        premiseDto.setOwnerId(owner.getId());

        premiseDto.setType(form.getType());
        premiseDto.setArea(form.getArea());
        premiseDto.setCapacity(form.getCapacity());
        premiseDto.setAmenities(form.getAmenities() != null ? form.getAmenities() : new ArrayList<>());

        premiseDto.setPriceWeekday(form.getPriceWeekday());
        premiseDto.setPriceWeekend(form.getPriceWeekend());
        premiseDto.setPriceHoliday(form.getPriceHoliday());

        premiseDto.setAvailableFrom(form.getAvailableFrom());
        premiseDto.setAvailableTo(form.getAvailableTo());

        premiseDto.setRegion(form.getRegion());
        premiseDto.setCity(form.getCity());
        premiseDto.setStreet(form.getStreet());
        premiseDto.setBuilding(form.getBuilding());
        premiseDto.setFloor(form.getFloor());
        premiseDto.setApartment(form.getApartment());

        premiseDto.setDescription(form.getDescription());
        premiseDto.setExtraFees(form.getExtraFees());
        premiseDto.setImportantInfo(form.getImportantInfo());

        premiseDto.setContactFirstName(getOrDefault(form.getOwnerFirstName(), owner.getFirstName()));
        premiseDto.setContactLastName(getOrDefault(form.getOwnerLastName(), owner.getLastName()));
        premiseDto.setContactMiddleName(getOrDefault(form.getOwnerMiddleName(), owner.getMiddleName()));
        premiseDto.setContactPhone(getOrDefault(form.getOwnerPhone(), owner.getPhone()));
        premiseDto.setContactEmail(getOrDefault(form.getOwnerEmail(), owner.getEmail()));

        if (photos != null && !photos.isEmpty() && !photos.stream().allMatch(MultipartFile::isEmpty)) {
            List<String> paths = fileStorageService.savePhotos(photos);
            premiseDto.setPhotoPaths(paths);
        }

        Double lat = null, lng = null;
        try {
            if (latitudeStr != null && !latitudeStr.trim().isEmpty()) lat = Double.parseDouble(latitudeStr.trim());
            if (longitudeStr != null && !longitudeStr.trim().isEmpty()) lng = Double.parseDouble(longitudeStr.trim());
        } catch (Exception e) {
            System.err.println("Ошибка парсинга координат");
        }

        premiseDto.setLatitude(lat);
        premiseDto.setLongitude(lng);

        catalogService.addPremise(premiseDto);

        return "redirect:/premise/add?success";
    }

    // ==================== ОБЪЯВЛЕНИЯ ПОЛЬЗОВАТЕЛЯ ====================

    @GetMapping("/profile/{username}/premises")
    public String userPremises(@PathVariable String username,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) String city,
                               HttpServletRequest request,
                               Model model) {

        User profileUser = userService.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        List<PremiseDto> userPremises = catalogService.getPremisesByOwnerId(profileUser.getId());

        // Загружаем аренды для каждого помещения
        for (PremiseDto premise : userPremises) {
            // Загружаем подтверждённые бронирования
            List<BookingDto> approvedBookings = bookingService.getApprovedBookingsWithDetails(premise.getId());
            premise.setBookings(approvedBookings);

            // Загружаем ожидающие запросы
            List<Map<String, Object>> pendingRequests = bookingService.getPendingRequestsWithDetails(premise.getId());
            premise.setPendingBookings(pendingRequests);
        }

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("premises", userPremises);
        model.addAttribute("isOwnProfile", userDetails != null && userDetails.getUsername().equals(username));

        return "future/user-premises";
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}