package com.example.rentapp.controller;

import com.example.rentapp.dto.NewPasswordRequest;
import com.example.rentapp.dto.PasswordResetRequest;
import com.example.rentapp.dto.VerifyCodeRequest;
import com.example.rentapp.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(HttpServletRequest request) {
        // Очищаем сохранённый URL, чтобы при логине не перенаправляло на страницу восстановления
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("redirectAfterLogin");
            System.out.println("Cleared redirectAfterLogin on forgot-password page");
        }
        return "auth/forgot-password";
    }

    @GetMapping("/verify-code")
    public String showVerifyCodeForm(@RequestParam String email, HttpServletRequest request, Model model) {
        // Очищаем сохранённый URL
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("redirectAfterLogin");
            System.out.println("Cleared redirectAfterLogin on verify-code page");
        }

        model.addAttribute("email", email);
        return "auth/verify-code";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String email, @RequestParam String code,
                                        HttpServletRequest request, Model model) {
        // Очищаем сохранённый URL
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("redirectAfterLogin");
            System.out.println("Cleared redirectAfterLogin on reset-password page");
        }

        model.addAttribute("email", email);
        model.addAttribute("code", code);
        return "auth/reset-password";
    }

    @PostMapping("/api/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendResetCode(@Valid @RequestBody PasswordResetRequest request) {
        Map<String, Object> response = new HashMap<>();

        boolean sent = passwordResetService.sendResetCode(request.getEmail());

        if (sent) {
            response.put("success", true);
            response.put("message", "Код восстановления отправлен на вашу почту");
            response.put("email", request.getEmail());
        } else {
            response.put("success", false);
            response.put("message", "Пользователь с таким email не найден");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/change-password-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePasswordRequest(@Valid @RequestBody PasswordResetRequest request) {
        Map<String, Object> response = new HashMap<>();

        boolean sent = passwordResetService.sendChangePasswordCode(request.getEmail());

        if (sent) {
            response.put("success", true);
            response.put("message", "Код для смены пароля отправлен на вашу почту");
            response.put("email", request.getEmail());
        } else {
            response.put("success", false);
            response.put("message", "Пользователь с таким email не найден");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        Map<String, Object> response = new HashMap<>();

        boolean valid = passwordResetService.verifyCode(request.getEmail(), request.getCode());

        if (valid) {
            response.put("success", true);
            response.put("message", "Код подтверждён");
            response.put("email", request.getEmail());
            response.put("code", request.getCode());
        } else {
            response.put("success", false);
            response.put("message", "Неверный или просроченный код");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "Пароли не совпадают");
            return ResponseEntity.badRequest().body(response);
        }

        boolean reset = passwordResetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        if (reset) {
            response.put("success", true);
            response.put("message", "Пароль успешно изменён");
        } else {
            response.put("success", false);
            response.put("message", "Не удалось изменить пароль. Попробуйте снова.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody NewPasswordRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "Пароли не совпадают");
            return ResponseEntity.badRequest().body(response);
        }

        boolean reset = passwordResetService.changePassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        if (reset) {
            response.put("success", true);
            response.put("message", "Пароль успешно изменён");
        } else {
            response.put("success", false);
            response.put("message", "Не удалось изменить пароль. Попробуйте снова.");
        }

        return ResponseEntity.ok(response);
    }
}