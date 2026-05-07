package com.example.rentapp.service;

import com.example.rentapp.dto.NotificationDto;
import com.example.rentapp.entity.Notification;
import com.example.rentapp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public int getUnreadNotificationsCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void createNotification(Long userId, String type, Long relatedId,
                                   Long fromUserId, String fromUserName,
                                   String content, String link) {
        Notification notification = new Notification(userId, type, relatedId,
                fromUserId, fromUserName, content, link);
        notificationRepository.save(notification);
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

    // НОВЫЙ МЕТОД: проверка наличия непрочитанного уведомления о сообщении
    public boolean hasUnreadMessageNotification(Long userId, Long fromUserId, Long premiseId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);

        for (Notification notif : notifications) {
            if ("MESSAGE".equals(notif.getType())) {
                // Проверяем, что уведомление от того же отправителя
                if (notif.getFromUserId() != null && notif.getFromUserId().equals(fromUserId)) {
                    // Если есть привязка к помещению, проверяем её
                    if (premiseId != null && notif.getRelatedId() != null && notif.getRelatedId().equals(premiseId)) {
                        return true;
                    }
                    // Если нет привязки к помещению (обычный чат)
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
}