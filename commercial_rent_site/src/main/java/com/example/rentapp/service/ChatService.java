package com.example.rentapp.service;

import com.example.rentapp.entity.ChatMessage;
import com.example.rentapp.entity.User;
import com.example.rentapp.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    public ChatMessage sendMessage(Long senderId, Long receiverId, String text) {
        User sender = userService.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        User receiver = userService.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                sender.getLogin(),
                text
        );

        ChatMessage saved = chatMessageRepository.save(message);

        String chatLink = "/chats/with/" + sender.getLogin();

        // ВСЕГДА создаем уведомление для первого сообщения в чате
        // Если у пользователя уже есть непрочитанные сообщения от этого отправителя,
        // новое уведомление не создаем (только одно уведомление на диалог)
        if (!notificationService.hasUnreadMessageNotification(receiverId, senderId, null)) {
            notificationService.createNotification(
                    receiverId,
                    "MESSAGE",
                    null,
                    senderId,
                    sender.getLogin(),
                    text.length() > 100 ? text.substring(0, 100) + "..." : text,
                    chatLink
            );
            System.out.println("✅ Создано уведомление о сообщении от " + sender.getLogin() + " для " + receiver.getLogin());
        } else {
            System.out.println("📌 Уведомление уже существует, пропускаем дубликат для диалога от " + sender.getLogin());
        }

        return saved;
    }

    public ChatMessage sendMessageWithPremise(Long senderId, Long receiverId, String text, Long premiseId) {
        User sender = userService.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        User receiver = userService.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                sender.getLogin(),
                text,
                premiseId
        );

        ChatMessage saved = chatMessageRepository.save(message);

        String chatLink = "/chats/with/" + sender.getLogin() + "/premise/" + premiseId;

        // ВСЕГДА создаем уведомление для первого сообщения в чате (по конкретному помещению)
        if (!notificationService.hasUnreadMessageNotification(receiverId, senderId, premiseId)) {
            notificationService.createNotification(
                    receiverId,
                    "MESSAGE",
                    premiseId,
                    senderId,
                    sender.getLogin(),
                    text.length() > 100 ? text.substring(0, 100) + "..." : text,
                    chatLink
            );
            System.out.println("✅ Создано уведомление о сообщении от " + sender.getLogin() + " по помещению #" + premiseId);
        } else {
            System.out.println("📌 Уведомление уже существует, пропускаем дубликат для помещения #" + premiseId);
        }

        return saved;
    }

    public ChatMessage sendMessageWithFile(Long senderId, Long receiverId, String text, Long premiseId,
                                           String fileName, String fileUrl, String fileType, Long fileSize) {
        User sender = userService.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        User receiver = userService.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                sender.getLogin(),
                text,
                premiseId,
                fileName,
                fileUrl,
                fileType,
                fileSize
        );

        ChatMessage saved = chatMessageRepository.save(message);

        String chatLink = (premiseId != null)
                ? "/chats/with/" + sender.getLogin() + "/premise/" + premiseId
                : "/chats/with/" + sender.getLogin();

        if (!notificationService.hasUnreadMessageNotification(receiverId, senderId, premiseId)) {
            notificationService.createNotification(
                    receiverId,
                    "MESSAGE",
                    premiseId,
                    senderId,
                    sender.getLogin(),
                    "📎 Отправлен файл: " + fileName,
                    chatLink
            );
        }

        return saved;
    }

    public ChatMessage sendSystemMessage(Long senderId, Long receiverId, String text) {
        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                "Система",
                text
        );
        return chatMessageRepository.save(message);
    }

    public ChatMessage sendSystemMessageWithPremise(Long senderId, Long receiverId, String text, Long premiseId) {
        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                "Система",
                text,
                premiseId
        );
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatBetween(Long userId1, Long userId2) {
        return chatMessageRepository.findChatBetweenUsers(userId1, userId2);
    }

    public List<ChatMessage> getChatBetweenByPremise(Long userId1, Long userId2, Long premiseId) {
        return chatMessageRepository.findChatBetweenUsersByPremise(userId1, userId2, premiseId);
    }

    public List<ChatMessage> getAllUserMessages(Long userId) {
        return chatMessageRepository.findAllMessagesForUser(userId);
    }

    public List<ChatMessage> getUnreadMessages(Long userId) {
        return chatMessageRepository.findByReceiverIdAndReadFalse(userId);
    }

    // НОВЫЙ МЕТОД: подсчет непрочитанных сообщений
    public int getUnreadMessagesCount(Long userId) {
        return chatMessageRepository.findByReceiverIdAndReadFalse(userId).size();
    }

    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        chatMessageRepository.markMessagesAsRead(otherUserId, currentUserId);
        System.out.println("Marked messages as read from user " + otherUserId);

        // После отметки о прочтении удаляем уведомление о непрочитанных сообщениях
        notificationService.deleteNotificationsForChat(currentUserId, otherUserId, null);
    }

    @Transactional
    public void markMessagesAsReadByPremise(Long currentUserId, Long otherUserId, Long premiseId) {
        chatMessageRepository.markMessagesAsReadByPremise(otherUserId, currentUserId, premiseId);
        System.out.println("Marked messages as read from user " + otherUserId + " for premise " + premiseId);

        // После отметки о прочтении удаляем уведомление о непрочитанных сообщениях
        notificationService.deleteNotificationsForChat(currentUserId, otherUserId, premiseId);
    }
}