package com.example.rentapp.config;

import com.example.rentapp.entity.User;
import com.example.rentapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByLogin("admin")) {
            User superAdmin = new User();
            superAdmin.setLogin("admin");
            superAdmin.setEmail("admin@mail.ru");
            superAdmin.setPassword(passwordEncoder.encode("admin"));
            superAdmin.setRole(User.Role.SUPER_ADMIN);

            userRepository.save(superAdmin);
            System.out.println("Супер-админ создан: login=admin, password=admin, email=admin@mail.ru");
        }
    }
}