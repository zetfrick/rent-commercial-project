package com.example.rentapp.service;

import com.example.rentapp.dto.BanRequestDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.entity.UserBan;
import com.example.rentapp.repository.UserBanRepository;
import com.example.rentapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserBanService {

    @Autowired
    private UserBanRepository userBanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public boolean banUser(Long adminId, BanRequestDto banRequest) {
        Optional<User> userOpt = userRepository.findById(banRequest.getUserId());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        User admin = userRepository.findById(adminId).orElse(null);
        if (admin == null) return false;

        // Нельзя заблокировать администратора
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            return false;
        }

        // Деактивируем текущую активную блокировку, если есть
        userBanRepository.deactivateActiveBan(banRequest.getUserId());

        LocalDateTime bannedUntil = calculateBanUntil(banRequest.getDuration(), banRequest.getCustomDate());

        UserBan ban = new UserBan(
                banRequest.getUserId(),
                adminId,
                banRequest.getReason(),
                bannedUntil
        );

        userBanRepository.save(ban);

        // Отправляем уведомление пользователю о блокировке
        String banMessage = String.format(
                "⛔ Ваш аккаунт был заблокирован администратором %s.\nПричина: %s\nБлокировка действует до %s",
                admin.getLogin(),
                banRequest.getReason(),
                formatDateTime(bannedUntil)
        );

        notificationService.createNotification(
                banRequest.getUserId(),
                "USER_BANNED",
                null,
                adminId,
                admin.getLogin(),
                banMessage,
                "/profile"
        );

        return true;
    }

    private LocalDateTime calculateBanUntil(String duration, LocalDateTime customDate) {
        LocalDateTime now = LocalDateTime.now();
        switch (duration) {
            case "1_DAY":
                return now.plusDays(1);
            case "1_WEEK":
                return now.plusWeeks(1);
            case "1_MONTH":
                return now.plusMonths(1);
            case "1_YEAR":
                return now.plusYears(1);
            case "CUSTOM":
                return customDate != null ? customDate : now.plusDays(1);
            default:
                return now.plusDays(1);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    @Transactional
    public boolean unbanUser(Long userId, Long adminId) {
        Optional<UserBan> banOpt = userBanRepository.findByUserIdAndActiveTrue(userId);
        if (banOpt.isEmpty()) return false;

        User admin = userRepository.findById(adminId).orElse(null);
        if (admin == null) return false;

        userBanRepository.deactivateActiveBan(userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            notificationService.createNotification(
                    userId,
                    "USER_UNBANNED",
                    null,
                    adminId,
                    admin.getLogin(),
                    "✅ Ваш аккаунт был разблокирован администратором " + admin.getLogin(),
                    "/profile"
            );
        }

        return true;
    }

    public boolean isUserBanned(Long userId) {
        Optional<UserBan> banOpt = userBanRepository.findByUserIdAndActiveTrue(userId);
        if (banOpt.isEmpty()) return false;

        UserBan ban = banOpt.get();
        if (ban.getBannedUntil().isBefore(LocalDateTime.now())) {
            // Блокировка истекла, деактивируем
            userBanRepository.deactivateActiveBan(userId);
            return false;
        }
        return true;
    }

    public Optional<UserBan> getActiveBan(Long userId) {
        return userBanRepository.findByUserIdAndActiveTrue(userId);
    }

    // Автоматическое удаление истекших блокировок (каждый час)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredBans() {
        int deleted = userBanRepository.deleteExpiredBans(LocalDateTime.now());
        if (deleted > 0) {
            System.out.println("Удалено просроченных блокировок: " + deleted);
        }
    }
}