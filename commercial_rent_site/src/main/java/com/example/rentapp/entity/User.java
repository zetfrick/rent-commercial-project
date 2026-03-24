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

    // === Личные данные (для личного кабинета) ===
    private String firstName;      // Имя
    private String lastName;       // Фамилия
    private String middleName;     // Отчество (может быть null)
    private String phone;          // Телефон (может быть null)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public enum Role {
        USER,
        ADMIN,
        SUPER_ADMIN
    }
}