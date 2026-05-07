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

        // Ссылка на чат с отправителем (тем, кто написал сообщение)
        String chatLink = "/chats/with/" + sender.getLogin();

        if (!notificationService.hasUnreadMessageNotification(receiverId, senderId, null)) {
            notificationService.createNotification(
                    receiverId,
                    "MESSAGE",
                    null,
                    senderId,
                    sender.getLogin(),
                    null,
                    chatLink
            );
            System.out.println("Создано новое уведомление о сообщении от " + sender.getLogin() + " для " + receiver.getLogin());
        } else {
            System.out.println("Уведомление уже существует, пропускаем дубликат");
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

        // ИСПРАВЛЕННАЯ ССЫЛКА - на чат с отправителем (кто написал сообщение)
        String chatLink = "/chats/with/" + sender.getLogin() + "/premise/" + premiseId;

        if (!notificationService.hasUnreadMessageNotification(receiverId, senderId, premiseId)) {
            notificationService.createNotification(
                    receiverId,
                    "MESSAGE",
                    premiseId,
                    senderId,
                    sender.getLogin(),
                    null,
                    chatLink
            );
            System.out.println("Создано новое уведомление о сообщении от " + sender.getLogin() + " по помещению #" + premiseId);
        } else {
            System.out.println("Уведомление уже существует, пропускаем дубликат");
        }

        return saved;
    }

    // НОВЫЙ МЕТОД: отправка системного сообщения
    public ChatMessage sendSystemMessage(Long senderId, Long receiverId, String text) {
        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                "Система",
                text
        );
        return chatMessageRepository.save(message);
    }

    // НОВЫЙ МЕТОД: отправка системного сообщения с привязкой к помещению
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

    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        chatMessageRepository.markMessagesAsRead(otherUserId, currentUserId);
        System.out.println("Marked messages as read from user " + otherUserId);
    }

    @Transactional
    public void markMessagesAsReadByPremise(Long currentUserId, Long otherUserId, Long premiseId) {
        chatMessageRepository.markMessagesAsReadByPremise(otherUserId, currentUserId, premiseId);
        System.out.println("Marked messages as read from user " + otherUserId + " for premise " + premiseId);
    }
}