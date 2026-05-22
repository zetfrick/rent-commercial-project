package com.example.rentapp.service;

import com.example.rentapp.dto.NotificationDto;
import com.example.rentapp.entity.Notification;
import com.example.rentapp.entity.User;
import com.example.rentapp.repository.NotificationRepository;
import com.example.rentapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public int getUnreadNotificationsCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void createNotification(Long userId, String type, Long relatedId,
                                   Long fromUserId, String fromUserName,
                                   String content, String link) {
        Notification notification = new Notification(userId, type, relatedId,
                fromUserId, fromUserName, content, link);
        notificationRepository.save(notification);

        // Отправляем email, если пользователь включил уведомления
        sendEmailNotification(userId, type, fromUserName, content, link, fromUserId);
    }

    // Добавьте этот метод в NotificationService.java

    @Transactional
    public void deleteNotificationsForChat(Long userId, Long fromUserId, Long premiseId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);

        for (Notification notif : notifications) {
            if ("MESSAGE".equals(notif.getType())) {
                if (notif.getFromUserId() != null && notif.getFromUserId().equals(fromUserId)) {
                    if (premiseId != null && notif.getRelatedId() != null && notif.getRelatedId().equals(premiseId)) {
                        notificationRepository.deleteById(notif.getId());
                        System.out.println("🗑️ Удалено уведомление о сообщении для чата с пользователем " + fromUserId);
                        break;
                    }
                    if (premiseId == null && notif.getRelatedId() == null) {
                        notificationRepository.deleteById(notif.getId());
                        System.out.println("🗑️ Удалено уведомление о сообщении для чата с пользователем " + fromUserId);
                        break;
                    }
                }
            }
        }
    }

    private void sendEmailNotification(Long userId, String type, String fromUserName, String content, String link, Long fromUserId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isEmailNotificationsEnabled()) {
                return;
            }

            if (mailSender == null) {
                System.out.println("Mail sender not configured, skipping email notification");
                return;
            }

            // Получаем полное имя отправителя
            String fromFullName = null;
            if (fromUserId != null) {
                User fromUser = userRepository.findById(fromUserId).orElse(null);
                if (fromUser != null) {
                    fromFullName = (fromUser.getFirstName() != null && fromUser.getLastName() != null)
                            ? fromUser.getFirstName() + " " + fromUser.getLastName()
                            : null;
                }
            }

            String subject = getEmailSubject(type);
            String emailBody = getEmailBody(type, fromUserName, content, link, fromFullName);

            if (subject != null && emailBody != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("zetfrick@mail.ru");
                message.setTo(user.getEmail());
                message.setSubject(subject);
                message.setText(emailBody);
                mailSender.send(message);
                System.out.println("Email уведомление отправлено на " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Ошибка отправки email уведомления: " + e.getMessage());
        }
    }

    private String getEmailSubject(String type) {
        switch (type) {
            case "MESSAGE":
                return "📨 Новое сообщение - Аренда помещений";
            case "PREMISE_EXPIRED":
                return "📋 Объявление снято с публикации - Аренда помещений";
            case "BOOKING_REQUEST":
                return "📅 Новый запрос на аренду - Аренда помещений";
            case "BOOKING_REJECTED":
                return "❌ Запрос на аренду отклонён - Аренда помещений";
            case "BOOKING_APPROVED":
                return "✅ Аренда подтверждена - Аренда помещений";
            case "BOOKING_CANCELLED_BY_OWNER":
                return "⚠️ Аренда отменена владельцем - Аренда помещений";
            case "BOOKING_CANCELLED_BY_RENTER":
                return "⚠️ Аренда отменена арендатором - Аренда помещений";
            case "BOOKING_STARTS_IN_3_DAYS":
                return "🔔 Аренда начнётся через 3 дня - Аренда помещений";
            case "BOOKING_STARTS_IN_1_DAY":
                return "🔔 Аренда начнётся завтра - Аренда помещений";
            case "BOOKING_STARTS_TODAY":
                return "🔔 Аренда начинается сегодня - Аренда помещений";
            case "BOOKING_ENDS_IN_3_DAYS":
                return "⚠️ Аренда заканчивается через 3 дня - Аренда помещений";
            case "BOOKING_ENDS_IN_1_DAY":
                return "⚠️ Аренда заканчивается завтра - Аренда помещений";
            case "BOOKING_ENDS_TODAY":
                return "⚠️ Аренда заканчивается сегодня - Аренда помещений";
            case "COMMENT":
                return "💬 Новый комментарий - Аренда помещений";
            case "AVAILABILITY_FREE":
                return "🔔 Даты освободились - Аренда помещений";
            default:
                return "Новое уведомление - Аренда помещений";
        }
    }

    private String getEmailBody(String type, String fromUserName, String content, String link, String fromFullName) {
        String baseUrl = "http://localhost:8080";

        // Декодируем content, если нужно
        String decodedContent = content;
        if (content != null && content.contains("%")) {
            try {
                decodedContent = URLDecoder.decode(content, StandardCharsets.UTF_8.toString());
                System.out.println("Декодирован content: " + decodedContent);
            } catch (Exception e) {
                System.err.println("Ошибка декодирования: " + e.getMessage());
                decodedContent = content;
            }
        }

        // Используем декодированную версию для отображения
        String displayContent = decodedContent != null ? decodedContent : "";

        // Используем отображаемое имя (если есть полное имя, иначе логин)
        String displayName = (fromFullName != null && !fromFullName.trim().isEmpty())
                ? fromFullName + " (" + fromUserName + ")"
                : (fromUserName != null ? fromUserName : "Пользователь");

        switch (type) {
            case "MESSAGE":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s отправил вам сообщение.\n\n" +
                                "Перейти к чату: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, baseUrl, link
                );
            case "PREMISE_EXPIRED":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "%s\n\n" +
                                "Подробнее: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayContent, baseUrl, link
                );
            case "BOOKING_REQUEST":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s отправил запрос на аренду вашего помещения.\n\n" +
                                "Подробности: %s\n\n" +
                                "Перейти к чату: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, displayContent, baseUrl, link
                );
            case "BOOKING_APPROVED":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s подтвердил аренду помещения.\n\n" +
                                "%s\n\n" +
                                "Перейти к чату: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, displayContent, baseUrl, link
                );
            case "BOOKING_REJECTED":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s отклонил ваш запрос на аренду.\n\n" +
                                "Перейти к чату: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, baseUrl, link
                );
            case "BOOKING_CANCELLED_BY_OWNER":
            case "BOOKING_CANCELLED_BY_RENTER":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s отменил аренду помещения.\n\n" +
                                "%s\n\n" +
                                "Подробнее: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, displayContent, baseUrl, link
                );
            case "COMMENT":
                return String.format(
                        "Здравствуйте!\n\n" +
                                "Пользователь %s оставил комментарий к вашему объявлению:\n\n" +
                                "«%s»\n\n" +
                                "Перейти к объявлению: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayName, displayContent, baseUrl, link
                );
            case "AVAILABILITY_FREE":
                // Удаляем HTML-теги из content для email (на случай если они есть)
                String plainContent = displayContent.replaceAll("<[^>]*>", "").trim();

                return String.format(
                        "Здравствуйте!\n\n" +
                                "%s\n\n" +
                                "Перейти к объявлению: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        plainContent, baseUrl, link
                );
            default:
                return String.format(
                        "Здравствуйте!\n\n" +
                                "%s\n\n" +
                                "Подробнее: %s%s\n\n" +
                                "С уважением,\nКоманда Аренда помещений",
                        displayContent, baseUrl, link
                );
        }
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false)
                .stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getReadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, true)
                .stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());
    }

    public boolean hasUnreadMessageNotification(Long userId, Long fromUserId, Long premiseId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);

        for (Notification notif : notifications) {
            if ("MESSAGE".equals(notif.getType())) {
                if (notif.getFromUserId() != null && notif.getFromUserId().equals(fromUserId)) {
                    if (premiseId != null && notif.getRelatedId() != null && notif.getRelatedId().equals(premiseId)) {
                        return true;
                    }
                    if (premiseId == null && notif.getRelatedId() == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Transactional
    public void markAsRead(Long userId, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            notificationRepository.markAsRead(userId, ids);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotifications(Long userId, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            notificationRepository.deleteByIds(userId, ids);
        }
    }

    @Transactional
    public void clearReadNotifications(Long userId) {
        List<Notification> readNotifications = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, true);
        if (!readNotifications.isEmpty()) {
            List<Long> ids = readNotifications.stream().map(Notification::getId).collect(Collectors.toList());
            notificationRepository.deleteByIds(userId, ids);
        }
    }

    @Transactional
    public int deleteOldReadNotifications(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return notificationRepository.deleteOldReadNotifications(userId, thirtyDaysAgo);
    }

    // НОВЫЙ МЕТОД: обновление настроек email-уведомлений
    @Transactional
    public void updateEmailNotificationsEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setEmailNotificationsEnabled(enabled);
            userRepository.save(user);
        }
    }

    // НОВЫЙ МЕТОД: получение статуса email-уведомлений
    public boolean isEmailNotificationsEnabled(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.isEmailNotificationsEnabled();
    }
}