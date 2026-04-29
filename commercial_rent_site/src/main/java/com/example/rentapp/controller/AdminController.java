package com.example.rentapp.controller;

import com.example.rentapp.entity.User;
import com.example.rentapp.repository.UserRepository;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    // Страница управления администраторами (только для SUPER_ADMIN)
    @GetMapping("/admins")
    public String adminsPage(Model model) {
        // Загружаем всех пользователей с ролью ADMIN или SUPER_ADMIN
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        model.addAttribute("admins", admins);
        return "future/admin-admins";
    }

    // API для получения списка администраторов (для AJAX)
    @GetMapping("/api/admins")
    @ResponseBody
    public List<User> getAdminsApi() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN)
                .collect(Collectors.toList());
    }

    // API для получения роли пользователя по логину
    @GetMapping("/api/user/role/{username}")
    @ResponseBody
    public String getUserRole(@PathVariable String username) {
        User user = userService.findByLogin(username).orElse(null);
        if (user != null) {
            return user.getRole().name();
        }
        return "USER";
    }

    // Добавление нового администратора
    @PostMapping("/api/admins/add")
    @ResponseBody
    public String addAdmin(@RequestParam String login,
                           @RequestParam String email,
                           @RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String password) {

        if (userRepository.existsByLogin(login)) {
            return "error: Логин уже существует";
        }
        if (userRepository.existsByEmail(email)) {
            return "error: Email уже существует";
        }

        User admin = new User();
        admin.setLogin(login);
        admin.setEmail(email);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);

        userRepository.save(admin);
        return "ok";
    }

    // Удаление администратора с принудительным завершением сессии
    @DeleteMapping("/api/admins/{id}")
    @ResponseBody
    public String deleteAdmin(@PathVariable Long id) {
        User admin = userRepository.findById(id).orElse(null);
        if (admin != null && admin.getRole() != User.Role.SUPER_ADMIN) {
            String adminLogin = admin.getLogin();

            // Завершаем все активные сессии пользователя
            List<Object> principals = sessionRegistry.getAllPrincipals();
            for (Object principal : principals) {
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    org.springframework.security.core.userdetails.User user =
                            (org.springframework.security.core.userdetails.User) principal;
                    if (user.getUsername().equals(adminLogin)) {
                        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                        for (SessionInformation session : sessions) {
                            session.expireNow(); // Завершаем сессию
                        }
                        break;
                    }
                }
            }

            userRepository.deleteById(id);
            return "ok";
        }
        return "error: Нельзя удалить главного администратора";
    }
}