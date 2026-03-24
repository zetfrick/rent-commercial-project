package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.dto.PremiseForm;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.FileStorageService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CatalogClient catalogClient;  // Клиент для вызова catalog-service

    // ==================== ПРОФИЛЬ ====================

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        model.addAttribute("currentUser", user);
        model.addAttribute("editMode", false);
        return "future/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        model.addAttribute("currentUser", user);
        model.addAttribute("editMode", true);
        return "future/profile";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute User updatedUser) {
        User currentUser = getCurrentUser(userDetails);

        if (!currentUser.getId().equals(updatedUser.getId())) {
            return "redirect:/profile?error";
        }

        currentUser.setFirstName(updatedUser.getFirstName());
        currentUser.setLastName(updatedUser.getLastName());
        currentUser.setMiddleName(updatedUser.getMiddleName());
        currentUser.setPhone(updatedUser.getPhone());

        userService.save(currentUser);
        return "redirect:/profile?success";
    }

    // ==================== ДОБАВЛЕНИЕ ПОМЕЩЕНИЯ ====================

    @GetMapping("/premise/add")
    public String addPremiseForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User owner = getCurrentUser(userDetails);

        PremiseForm form = new PremiseForm();
        // Автозаполнение контактов из профиля пользователя
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
                             @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {

        User owner = getCurrentUser(userDetails);

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

        // Контакты — приоритет у формы, иначе из профиля
        premiseDto.setContactFirstName(getOrDefault(form.getOwnerFirstName(), owner.getFirstName()));
        premiseDto.setContactLastName(getOrDefault(form.getOwnerLastName(), owner.getLastName()));
        premiseDto.setContactMiddleName(getOrDefault(form.getOwnerMiddleName(), owner.getMiddleName()));
        premiseDto.setContactPhone(getOrDefault(form.getOwnerPhone(), owner.getPhone()));
        premiseDto.setContactEmail(getOrDefault(form.getOwnerEmail(), owner.getEmail()));

        // Фото
        if (photos != null && !photos.isEmpty() && !photos.stream().allMatch(MultipartFile::isEmpty)) {
            List<String> paths = fileStorageService.savePhotos(photos);
            premiseDto.setPhotoPaths(paths);
        }

        // Отправляем помещение в catalog-service
        catalogClient.addPremise(premiseDto);

        return "redirect:/premise/add?success";
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}