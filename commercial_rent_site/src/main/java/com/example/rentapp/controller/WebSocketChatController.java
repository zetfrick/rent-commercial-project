package com.example.rentapp.controller;

import com.example.rentapp.dto.WebSocketMessageDto;
import com.example.rentapp.entity.ChatMessage;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.ChatService;
import com.example.rentapp.service.NotificationService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebSocketMessageDto message, Principal principal) {
        try {
            System.out.println("========================================");
            System.out.println("=== WebSocket收到消息 ===");
            System.out.println("Отправитель ID: " + message.getSenderId());
            System.out.println("Получатель ID: " + message.getReceiverId());
            System.out.println("Текст: " + message.getText());
            System.out.println("Principal: " + (principal != null ? principal.getName() : "null"));

            // Получаем пользователей
            User sender = userService.findById(message.getSenderId()).orElse(null);
            User receiver = userService.findById(message.getReceiverId()).orElse(null);

            if (sender == null || receiver == null) {
                System.err.println("❌ Пользователь не найден! sender=" + sender + ", receiver=" + receiver);
                return;
            }

            String senderUsername = sender.getLogin();
            String receiverUsername = receiver.getLogin();

            System.out.println("Отправитель (логин): " + senderUsername);
            System.out.println("Получатель (логин): " + receiverUsername);

            // Сохраняем сообщение в БД
            ChatMessage savedMessage;
            if (message.getPremiseId() != null && message.getPremiseId() > 0) {
                savedMessage = chatService.sendMessageWithPremise(
                        message.getSenderId(),
                        message.getReceiverId(),
                        message.getText(),
                        message.getPremiseId()
                );
            } else {
                savedMessage = chatService.sendMessage(
                        message.getSenderId(),
                        message.getReceiverId(),
                        message.getText()
                );
            }

            // Создаём DTO для отправки
            WebSocketMessageDto response = new WebSocketMessageDto(
                    savedMessage.getId(),
                    savedMessage.getSenderId(),
                    savedMessage.getReceiverId(),
                    savedMessage.getSenderLogin(),
                    savedMessage.getText(),
                    savedMessage.getSentAt(),
                    savedMessage.getPremiseId(),
                    "MESSAGE"
            );

            // Отправляем получателю по ЛОГИНУ
            System.out.println("📤 Отправка получателю (логин): " + receiverUsername);
            System.out.println("📤 Destination: /user/" + receiverUsername + "/queue/messages");
            messagingTemplate.convertAndSendToUser(
                    receiverUsername,
                    "/queue/messages",
                    response
            );
            System.out.println("✅ convertAndSendToUser вызван для: " + receiverUsername);

            // Отправляем отправителю
            messagingTemplate.convertAndSendToUser(
                    senderUsername,
                    "/queue/messages",
                    response
            );
            System.out.println("✅ convertAndSendToUser вызван для отправителя: " + senderUsername);
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ WebSocket error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WebSocketMessageDto message) {
        try {
            User receiver = userService.findById(message.getReceiverId()).orElse(null);
            if (receiver != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getLogin(),
                        "/queue/typing",
                        message
                );
            }
        } catch (Exception e) {
            System.err.println("Typing error: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload WebSocketMessageDto message) {
        try {
            if (message.getPremiseId() != null && message.getPremiseId() > 0) {
                chatService.markMessagesAsReadByPremise(
                        message.getReceiverId(),
                        message.getSenderId(),
                        message.getPremiseId()
                );
            } else {
                chatService.markMessagesAsRead(
                        message.getReceiverId(),
                        message.getSenderId()
                );
            }

            User sender = userService.findById(message.getSenderId()).orElse(null);
            if (sender != null) {
                messagingTemplate.convertAndSendToUser(
                        sender.getLogin(),
                        "/queue/read",
                        message
                );
            }
        } catch (Exception e) {
            System.err.println("Error marking messages as read: " + e.getMessage());
        }
    }
}