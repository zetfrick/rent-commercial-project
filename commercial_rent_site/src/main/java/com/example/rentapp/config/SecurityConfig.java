package com.example.rentapp.config;

import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {

                String redirectUrl = "/main/landing-logged"; // URL по умолчанию

                // Пытаемся получить сохранённый URL из сессии
                HttpSession session = request.getSession(false);
                if (session != null) {
                    String savedUrl = (String) session.getAttribute("redirectAfterLogin");
                    if (savedUrl != null && !savedUrl.isEmpty()) {
                        redirectUrl = savedUrl;
                        session.removeAttribute("redirectAfterLogin"); // очищаем после использования
                        System.out.println("=== LOGIN SUCCESS ===");
                        System.out.println("Redirecting to saved URL: " + redirectUrl);
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
                        .ignoringRequestMatchers("/api/**", "/chats/**", "/admin/**")
                )
                .authorizeHttpRequests(authz -> authz
                        // Публичные страницы - доступны всем
                        .requestMatchers("/", "/auth/**", "/catalog", "/catalog/**", "/about", "/contacts",
                                "/main/**", "/catalog/preview", "/uploads/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api/auth/register", "/api/auth/login",
                                "/auth/check",
                                "/premise/**")  // Просмотр объявлений доступен всем
                        .permitAll()

                        // Защищённые страницы - только для авторизованных
                        .requestMatchers("/profile", "/profile/**", "/api/premise/**",
                                "/chats/**")
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
                .userDetailsService(userService);

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