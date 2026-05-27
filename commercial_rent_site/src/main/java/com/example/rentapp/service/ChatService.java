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
        message.setDeliveryStatus("DELIVERED");

        ChatMessage saved = chatMessageRepository.save(message);

        String chatLink = "/chats/with/" + sender.getLogin();

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
        message.setDeliveryStatus("DELIVERED");

        ChatMessage saved = chatMessageRepository.save(message);

        String chatLink = "/chats/with/" + sender.getLogin() + "/premise/" + premiseId;

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
        message.setDeliveryStatus("DELIVERED");

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

    // НОВЫЙ МЕТОД: обновление статуса доставки сообщения
    @Transactional
    public void updateMessageDeliveryStatus(Long messageId, String status) {
        chatMessageRepository.updateDeliveryStatus(messageId, status);
    }

    // НОВЫЙ МЕТОД: обновление статусов всех сообщений в диалоге
    @Transactional
    public void updateMessagesStatusBetweenUsers(Long senderId, Long receiverId, Long premiseId, String status) {
        if (premiseId != null) {
            chatMessageRepository.updateDeliveryStatusByUsersAndPremise(senderId, receiverId, premiseId, status);
        } else {
            chatMessageRepository.updateDeliveryStatusByUsers(senderId, receiverId, status);
        }
    }

    public ChatMessage sendSystemMessage(Long senderId, Long receiverId, String text) {
        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                "Система",
                text
        );
        message.setDeliveryStatus("DELIVERED");
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
        message.setDeliveryStatus("DELIVERED");
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

    public int getUnreadMessagesCount(Long userId) {
        return chatMessageRepository.findByReceiverIdAndReadFalse(userId).size();
    }

    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        chatMessageRepository.markMessagesAsRead(otherUserId, currentUserId);

        // После отметки о прочтении обновляем статус доставки
        chatMessageRepository.updateDeliveryStatusByUsers(otherUserId, currentUserId, "READ");

        notificationService.deleteNotificationsForChat(currentUserId, otherUserId, null);
    }

    @Transactional
    public void markMessagesAsReadByPremise(Long currentUserId, Long otherUserId, Long premiseId) {
        chatMessageRepository.markMessagesAsReadByPremise(otherUserId, currentUserId, premiseId);

        // После отметки о прочтении обновляем статус доставки
        chatMessageRepository.updateDeliveryStatusByUsersAndPremise(otherUserId, currentUserId, premiseId, "READ");

        notificationService.deleteNotificationsForChat(currentUserId, otherUserId, premiseId);
    }

    /**
     * Получить все файловые сообщения в чате
     */
    public List<ChatMessage> getFileMessagesBetween(Long userId1, Long userId2, Long premiseId) {
        if (premiseId != null) {
            return chatMessageRepository.findFileMessagesBetweenUsersByPremise(userId1, userId2, premiseId);
        }
        return chatMessageRepository.findFileMessagesBetweenUsers(userId1, userId2);
    }
}