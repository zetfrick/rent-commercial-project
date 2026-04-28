package com.example.rentapp.config;

import com.example.rentapp.entity.User;
import com.example.rentapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Optional<User> existingAdmin = userRepository.findByLogin("admin");

        if (existingAdmin.isPresent()) {
            // Обновляем существующего админа
            User admin = existingAdmin.get();
            boolean updated = false;

            if (admin.getFirstName() == null || admin.getFirstName().isEmpty()) {
                admin.setFirstName("Админ");
                updated = true;
            }
            if (admin.getLastName() == null || admin.getLastName().isEmpty()) {
                admin.setLastName("Админов");
                updated = true;
            }
            if (admin.getPhone() == null || admin.getPhone().isEmpty()) {
                admin.setPhone("+7 999 123 45 67");
                updated = true;
            }

            if (updated) {
                userRepository.save(admin);
                System.out.println("Данные админа обновлены: Имя=Админ, Фамилия=Админов, Телефон=+7 999 123 45 67");
            } else {
                System.out.println("Данные админа уже заполнены");
            }
        } else {
            // Создаём нового админа
            User superAdmin = new User();
            superAdmin.setLogin("admin");
            superAdmin.setEmail("admin@mail.ru");
            superAdmin.setPassword(passwordEncoder.encode("admin"));
            superAdmin.setRole(User.Role.SUPER_ADMIN);
            superAdmin.setFirstName("Админ");
            superAdmin.setLastName("Админов");
            superAdmin.setPhone("+7 999 123 45 67");

            userRepository.save(superAdmin);
            System.out.println("Супер-админ создан: login=admin, password=admin, email=admin@mail.ru");
            System.out.println("Имя: Админ, Фамилия: Админов, Телефон: +7 999 123 45 67");
        }
    }
}