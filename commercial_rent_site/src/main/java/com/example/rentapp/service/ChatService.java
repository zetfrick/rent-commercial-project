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

    public ChatMessage sendMessage(Long senderId, Long receiverId, String text) {
        User sender = userService.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));

        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                sender.getLogin(),
                text
        );

        return chatMessageRepository.save(message);
    }

    public ChatMessage sendMessageWithPremise(Long senderId, Long receiverId, String text, Long premiseId) {
        User sender = userService.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));

        ChatMessage message = new ChatMessage(
                senderId,
                receiverId,
                sender.getLogin(),
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