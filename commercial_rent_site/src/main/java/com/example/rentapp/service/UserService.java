package com.example.rentapp.service;

import com.example.rentapp.entity.User;
import com.example.rentapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Поиск пользователя по логину (для личного кабинета)
    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    // Проверка, занят ли логин
    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    // Проверка, занят ли email
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Регистрация нового пользователя (с именем и фамилией)
    public User registerUser(String login, String email, String rawPassword,
                             String firstName, String lastName) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }

    // Сохранение изменений в профиле (редактирование)
    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Загрузка пользователя для Spring Security
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}