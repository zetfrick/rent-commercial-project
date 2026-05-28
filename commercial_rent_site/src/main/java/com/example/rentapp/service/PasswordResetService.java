package com.example.rentapp.service;

import com.example.rentapp.entity.PasswordResetToken;
import com.example.rentapp.entity.User;
import com.example.rentapp.repository.PasswordResetTokenRepository;
import com.example.rentapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private static final int CODE_EXPIRY_MINUTES = 5;

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    // ==================== ДЛЯ ВОССТАНОВЛЕНИЯ ПАРОЛЯ (ЗАБЫЛ ПАРОЛЬ) ====================

    @Transactional
    public boolean sendResetCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь с email " + email + " не найден");
            return false;
        }

        User user = userOpt.get();
        System.out.println("✅ Найден пользователь: " + user.getLogin() + " (" + user.getEmail() + ")");

        tokenRepository.deleteByEmail(email);

        String code = generateCode();
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(CODE_EXPIRY_MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken(email, token, code, now, expiresAt);
        tokenRepository.save(resetToken);

        System.out.println("Создан код восстановления для " + email + ": " + code + " (действителен до " + expiresAt + ")");

        try {
            sendResetCodeEmail(email, code);
            System.out.println("✅ Email отправлен на " + email);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки email: " + e.getMessage());
            return false;
        }
    }

    private void sendResetCodeEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("zetfrick@mail.ru");
        message.setTo(email);
        message.setSubject("🔐 Восстановление пароля - Аренда помещений");
        message.setText("Здравствуйте!\n\n" +
                "Вы запросили ВОССТАНОВЛЕНИЕ пароля на сайте Аренда помещений.\n\n" +
                "Ваш код для восстановления пароля: " + code + "\n\n" +
                "Код действителен в течение " + CODE_EXPIRY_MINUTES + " минут.\n\n" +
                "Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.\n\n" +
                "С уважением,\nКоманда Аренда помещений");

        try {
            mailSender.send(message);
            System.out.println("✅ Письмо для восстановления пароля отправлено на " + email);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке письма: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ДЛЯ СМЕНЫ ПАРОЛЯ (ИЗ ПРОФИЛЯ) ====================

    @Transactional
    public boolean sendChangePasswordCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь с email " + email + " не найден");
            return false;
        }

        User user = userOpt.get();
        System.out.println("✅ Найден пользователь: " + user.getLogin() + " (" + user.getEmail() + ")");

        tokenRepository.deleteByEmail(email);

        String code = generateCode();
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(CODE_EXPIRY_MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken(email, token, code, now, expiresAt);
        tokenRepository.save(resetToken);

        System.out.println("Создан код для смены пароля для " + email + ": " + code + " (действителен до " + expiresAt + ")");

        try {
            sendChangePasswordCodeEmail(email, code);
            System.out.println("✅ Email для смены пароля отправлен на " + email);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки email: " + e.getMessage());
            return false;
        }
    }

    private void sendChangePasswordCodeEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("zetfrick@mail.ru");
        message.setTo(email);
        message.setSubject("🔑 Смена пароля - Аренда помещений");
        message.setText("Здравствуйте!\n\n" +
                "Вы запросили СМЕНУ пароля в личном кабинете на сайте Аренда помещений.\n\n" +
                "Ваш код для смены пароля: " + code + "\n\n" +
                "Код действителен в течение " + CODE_EXPIRY_MINUTES + " минут.\n\n" +
                "Если вы не запрашивали смену пароля, обратитесь в службу поддержки.\n\n" +
                "С уважением,\nКоманда Аренда помещений");

        try {
            mailSender.send(message);
            System.out.println("✅ Письмо для смены пароля отправлено на " + email);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при отправке письма: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);
        if (tokenOpt.isEmpty()) {
            System.out.println("❌ Код не найден для email: " + email + ", code: " + code);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            System.out.println("❌ Код просрочен для email: " + email);
            tokenRepository.delete(resetToken);
            return false;
        }

        System.out.println("✅ Код подтверждён для email: " + email);
        return true;
    }

    @Transactional
    public boolean resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            System.out.println("❌ Пароли не совпадают");
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);
        if (tokenOpt.isEmpty()) {
            System.out.println("❌ Токен не найден для email: " + email);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            System.out.println("❌ Токен просрочен для email: " + email);
            tokenRepository.delete(resetToken);
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь не найден с email: " + email);
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        tokenRepository.deleteByEmail(email);

        System.out.println("✅ Пароль успешно изменён для пользователя: " + user.getLogin());
        return true;
    }

    @Transactional
    public boolean changePassword(String email, String code, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            System.out.println("❌ Пароли не совпадают");
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);
        if (tokenOpt.isEmpty()) {
            System.out.println("❌ Токен не найден для email: " + email);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            System.out.println("❌ Токен просрочен для email: " + email);
            tokenRepository.delete(resetToken);
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("❌ Пользователь не найден с email: " + email);
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        tokenRepository.deleteByEmail(email);

        System.out.println("✅ Пароль успешно изменён для пользователя: " + user.getLogin());
        return true;
    }
}