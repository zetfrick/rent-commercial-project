package com.example.rentapp.controller;

import com.example.rentapp.dto.RegisterRequest;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (userService.existsByLogin(request.getLogin())) {
            model.addAttribute("error", "Логин уже занят");
            return "auth/register";
        }

        if (userService.existsByEmail(request.getEmail())) {
            model.addAttribute("error", "Этот email уже зарегистрирован");
            return "auth/register";
        }

        // Создаём и сохраняем пользователя
        userService.registerUser(
                request.getLogin(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        return "redirect:/auth/login?registered";
    }
}