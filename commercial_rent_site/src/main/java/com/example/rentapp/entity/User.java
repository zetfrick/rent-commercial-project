package com.example.rentapp.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // === Личные данные ===
    private String firstName;
    private String lastName;
    private String middleName;
    private String phone;

    // === НАСТРОЙКИ УВЕДОМЛЕНИЙ ===
    @Column(nullable = false)
    private boolean emailNotificationsEnabled = true;  // ← НОВОЕ ПОЛЕ, по умолчанию включено

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public enum Role {
        USER,
        ADMIN,
        SUPER_ADMIN
    }
}