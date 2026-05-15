package com.example.rentapp.config;

import com.example.rentapp.service.CustomUserDetailsService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired @Lazy private UserService userService;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {

                String redirectUrl = "/main/landing-logged";

                HttpSession session = request.getSession(false);
                if (session != null) {
                    String savedUrl = (String) session.getAttribute("redirectAfterLogin");

                    // Проверяем, не является ли сохранённый URL страницей восстановления/смены пароля
                    if (savedUrl != null && !savedUrl.isEmpty()) {
                        // Список URL, которые нужно игнорировать (страницы восстановления пароля)
                        boolean isPasswordResetUrl = savedUrl.contains("/auth/reset-password") ||
                                savedUrl.contains("/auth/verify-code") ||
                                savedUrl.contains("/auth/forgot-password") ||
                                savedUrl.contains("code=") ||
                                savedUrl.contains("reset=success");

                        if (isPasswordResetUrl) {
                            // Игнорируем URL восстановления пароля
                            session.removeAttribute("redirectAfterLogin");
                            System.out.println("=== LOGIN SUCCESS ===");
                            System.out.println("Ignoring password reset URL: " + savedUrl);
                            System.out.println("Redirecting to default: " + redirectUrl);
                        } else {
                            redirectUrl = savedUrl;
                            session.removeAttribute("redirectAfterLogin");
                            System.out.println("=== LOGIN SUCCESS ===");
                            System.out.println("Redirecting to saved URL: " + redirectUrl);
                        }
                    } else {
                        System.out.println("=== LOGIN SUCCESS ===");
                        System.out.println("No saved URL, redirecting to default: " + redirectUrl);
                    }
                } else {
                    System.out.println("=== LOGIN SUCCESS ===");
                    System.out.println("No session, redirecting to default: " + redirectUrl);
                }

                response.sendRedirect(redirectUrl);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/chats/**",
                                "/admin/**",
                                "/notifications/api/**",
                                "/auth/api/**",
                                "/ws-chat/**"  // WebSocket endpoints
                        )
                )
                .authorizeHttpRequests(authz -> authz
                        // Публичные страницы - доступны всем
                        .requestMatchers("/", "/auth/**", "/catalog", "/catalog/**", "/about", "/contacts",
                                "/main/**", "/catalog/preview", "/uploads/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api/auth/register", "/api/auth/login",
                                "/auth/check",
                                "/premise/**")
                        .permitAll()

                        // API для восстановления пароля
                        .requestMatchers("/auth/api/forgot-password", "/auth/api/verify-code",
                                "/auth/api/reset-password", "/auth/api/change-password-request",
                                "/auth/api/change-password")
                        .permitAll()

                        // Разрешаем доступ к API уведомлений без авторизации
                        .requestMatchers("/notifications/api/create").permitAll()
                        .requestMatchers("/notifications/api/**").permitAll()

                        // ========== WEBSOCKET ENDPOINTS ==========
                        // Разрешаем WebSocket соединения без авторизации (авторизация внутри)
                        .requestMatchers("/ws-chat", "/ws-chat/**", "/ws-chat/info/**", "/ws-chat/info")
                        .permitAll()

                        // Защищённые страницы - только для авторизованных
                        .requestMatchers("/profile", "/profile/**", "/api/premise/**",
                                "/chats/**", "/notifications")
                        .authenticated()

                        // Добавление и редактирование помещений - только для авторизованных
                        .requestMatchers("/premise/add", "/premise/*/edit")
                        .authenticated()

                        // Админ-панель - только для ADMIN и SUPER_ADMIN
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                )
                .authenticationProvider(authenticationProvider())
                .userDetailsService(customUserDetailsService);

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}