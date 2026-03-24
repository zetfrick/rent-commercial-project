package com.example.rentapp.service;

import com.example.rentapp.entity.User;
import com.example.rentapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // Успешные сценарии регистрации

    @Test
    void registerUser_success_normalCase() {
        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User created = userService.registerUser(
                "newuser", "newuser@example.com", "secret123", "Анна", "Смирнова"
        );

        assertNotNull(created);
        assertEquals("newuser", created.getLogin());
        assertEquals("newuser@example.com", created.getEmail());
        assertEquals("hashed-secret123", created.getPassword());
        assertEquals("Анна", created.getFirstName());
        assertEquals("Смирнова", created.getLastName());
        assertEquals(User.Role.USER, created.getRole());
        verify(userRepository).save(any());
    }

    @Test
    void registerUser_passwordIsEncoded() {
        when(userRepository.existsByLogin(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded!!!");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User u = userService.registerUser("u", "u@mail.ru", "plain", "A", "B");

        assertEquals("encoded!!!", u.getPassword());
    }

    // Проверка существования

    @Test
    void existsByLogin_true() {
        when(userRepository.existsByLogin("admin")).thenReturn(true);
        assertTrue(userService.existsByLogin("admin"));
    }

    @Test
    void existsByLogin_false() {
        when(userRepository.existsByLogin("nonexistent")).thenReturn(false);
        assertFalse(userService.existsByLogin("nonexistent"));
    }

    @Test
    void existsByEmail_true() {
        when(userRepository.existsByEmail("admin@site.ru")).thenReturn(true);
        assertTrue(userService.existsByEmail("admin@site.ru"));
    }

    @Test
    void existsByEmail_false() {
        when(userRepository.existsByEmail("nobody@mail.com")).thenReturn(false);
        assertFalse(userService.existsByEmail("nobody@mail.com"));
    }

    // Поиск и загрузка для Security

    @Test
    void findByLogin_found() {
        User u = new User();
        u.setLogin("alice");
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(u));

        Optional<User> result = userService.findByLogin("alice");
        assertTrue(result.isPresent());
        assertSame(u, result.get());
    }

    @Test
    void findByLogin_notFound() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());
        assertTrue(userService.findByLogin("ghost").isEmpty());
    }

    @Test
    void loadUserByUsername_success() {
        User u = new User();
        u.setLogin("bob");
        u.setPassword("pass_hash");
        u.setRole(User.Role.USER);

        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(u));

        var details = userService.loadUserByUsername("bob");

        assertEquals("bob", details.getUsername());
        assertEquals("pass_hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByLogin("stranger")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("stranger"));
    }

    // Сохранение существующего пользователя

    @Test
    void save_existingUser() {
        User user = new User();
        user.setId(42L);
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.save(user);

        assertSame(user, saved);
        verify(userRepository).save(user);
    }
}