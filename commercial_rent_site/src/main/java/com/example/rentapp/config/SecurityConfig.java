// src/main/java/com/example/rentapp/config/SecurityConfig.java
package com.example.rentapp.config;

import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired @Lazy private UserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF для API и чатов
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/chats/**")  // Отключаем CSRF для всех чатов
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/auth/**", "/catalog", "/catalog/**", "/about", "/contacts",
                                "/main/**", "/catalog/preview", "/uploads/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api/auth/register", "/api/auth/login",
                                "/auth/check")
                        .permitAll()
                        .requestMatchers("/profile", "/profile/**", "/premise/**", "/api/premise/**",
                                "/chats/**")
                        .authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/main/landing-logged", true)
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .userDetailsService(userService);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}