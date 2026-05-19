package com.example.rentapp.controller;

import com.example.rentapp.dto.WebSocketMessageDto;
import com.example.rentapp.entity.ChatMessage;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.ChatService;
import com.example.rentapp.service.FileStorageService;
import com.example.rentapp.service.NotificationService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private FileStorageService fileStorageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebSocketMessageDto message, Principal principal) {
        try {
            System.out.println("========================================");
            System.out.println("=== WebSocket收到消息 ===");
            System.out.println("Отправитель ID: " + message.getSenderId());
            System.out.println("Получатель ID: " + message.getReceiverId());
            System.out.println("Текст: " + message.getText());
            System.out.println("Principal: " + (principal != null ? principal.getName() : "null"));

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

            System.out.println("📤 Отправка получателю (логин): " + receiverUsername);
            System.out.println("📤 Destination: /user/" + receiverUsername + "/queue/messages");
            messagingTemplate.convertAndSendToUser(
                    receiverUsername,
                    "/queue/messages",
                    response
            );
            System.out.println("✅ convertAndSendToUser вызван для: " + receiverUsername);

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

    @PostMapping("/chats/send-file")
    @ResponseBody
    public Map<String, Object> sendFile(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long premiseId,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws Exception {

        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return response;
        }

        User sender = userService.findByLogin(principal.getName()).orElse(null);
        if (sender == null) {
            response.put("success", false);
            response.put("message", "Отправитель не найден");
            return response;
        }

        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Файл не выбран");
            return response;
        }

        // Ограничение размера файла (10 MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            response.put("success", false);
            response.put("message", "Файл слишком большой (макс. 10 МБ)");
            return response;
        }

        // Сохраняем файл
        String fileName = fileStorageService.saveChatFile(file);
        String fileUrl = "/chat-uploads/" + fileName;

        // Определяем тип файла и создаём HTML для отображения
        String fileHtml = generateFileHtml(fileName, fileUrl, file.getContentType(), file.getSize());

        // Сохраняем сообщение с файлом в БД (текст содержит HTML для отображения файла)
        ChatMessage savedMessage;
        if (premiseId != null && premiseId > 0) {
            savedMessage = chatService.sendMessageWithPremise(
                    sender.getId(),
                    receiverId,
                    fileHtml,
                    premiseId
            );
        } else {
            savedMessage = chatService.sendMessage(
                    sender.getId(),
                    receiverId,
                    fileHtml
            );
        }

        // Отправляем через WebSocket, если соединение активно
        try {
            User receiver = userService.findById(receiverId).orElse(null);
            if (receiver != null) {
                WebSocketMessageDto wsMessage = new WebSocketMessageDto(
                        savedMessage.getId(),
                        savedMessage.getSenderId(),
                        savedMessage.getReceiverId(),
                        savedMessage.getSenderLogin(),
                        savedMessage.getText(),
                        savedMessage.getSentAt(),
                        savedMessage.getPremiseId(),
                        "FILE"
                );
                messagingTemplate.convertAndSendToUser(
                        receiver.getLogin(),
                        "/queue/messages",
                        wsMessage
                );
                messagingTemplate.convertAndSendToUser(
                        sender.getLogin(),
                        "/queue/messages",
                        wsMessage
                );
            }
        } catch (Exception e) {
            System.err.println("WebSocket send error: " + e.getMessage());
        }

        response.put("success", true);
        response.put("messageId", savedMessage.getId());
        response.put("fileHtml", fileHtml);
        response.put("fileName", fileName);
        response.put("fileUrl", fileUrl);
        return response;
    }

    private String generateFileHtml(String fileName, String fileUrl, String contentType, long fileSize) {
        String sizeStr = formatFileSize(fileSize);
        String fileExtension = getFileExtension(fileName).toLowerCase();

        // Иконка в зависимости от типа файла
        String iconHtml;
        String previewHtml = "";

        if (contentType != null && contentType.startsWith("image/")) {
            // Для изображений показываем миниатюру
            iconHtml = "<i class='fas fa-image' style='font-size: 24px; color: #ff5722;'></i>";
            previewHtml = "<div class='file-preview-image'><img src='" + fileUrl + "' alt='preview' onclick='openFullscreen(\"" + fileUrl + "\")'></div>";
        } else if (contentType != null && contentType.equals("application/pdf")) {
            iconHtml = "<i class='fas fa-file-pdf' style='font-size: 24px; color: #e74c3c;'></i>";
        } else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
            iconHtml = "<i class='fas fa-file-word' style='font-size: 24px; color: #2b5797;'></i>";
        } else if (fileExtension.equals("xls") || fileExtension.equals("xlsx")) {
            iconHtml = "<i class='fas fa-file-excel' style='font-size: 24px; color: #217346;'></i>";
        } else if (fileExtension.equals("ppt") || fileExtension.equals("pptx")) {
            iconHtml = "<i class='fas fa-file-powerpoint' style='font-size: 24px; color: #d35230;'></i>";
        } else if (fileExtension.equals("zip") || fileExtension.equals("rar") || fileExtension.equals("7z")) {
            iconHtml = "<i class='fas fa-file-archive' style='font-size: 24px; color: #f39c12;'></i>";
        } else if (fileExtension.equals("mp3") || fileExtension.equals("wav") || fileExtension.equals("ogg")) {
            iconHtml = "<i class='fas fa-file-audio' style='font-size: 24px; color: #3498db;'></i>";
        } else if (fileExtension.equals("mp4") || fileExtension.equals("avi") || fileExtension.equals("mov")) {
            iconHtml = "<i class='fas fa-file-video' style='font-size: 24px; color: #9b59b6;'></i>";
        } else {
            iconHtml = "<i class='fas fa-file' style='font-size: 24px; color: #7f8c8d;'></i>";
        }

        return "<div class='file-attachment' data-file-url='" + fileUrl + "' data-file-name='" + escapeHtml(fileName) + "'>" +
                "<div class='file-icon'>" + iconHtml + "</div>" +
                "<div class='file-info'>" +
                "<div class='file-name' title='" + escapeHtml(fileName) + "'>" + truncateFileName(fileName, 30) + "</div>" +
                "<div class='file-size'>" + sizeStr + "</div>" +
                "</div>" +
                "<a href='" + fileUrl + "' download class='file-download-btn'><i class='fas fa-download'></i></a>" +
                (previewHtml.isEmpty() ? "" : previewHtml) +
                "</div>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private String truncateFileName(String fileName, int maxLength) {
        if (fileName.length() <= maxLength) return fileName;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String name = fileName.substring(0, lastDot);
            String ext = fileName.substring(lastDot);
            if (name.length() > maxLength - ext.length() - 3) {
                name = name.substring(0, maxLength - ext.length() - 3) + "...";
            }
            return name + ext;
        }
        return fileName.substring(0, maxLength - 3) + "...";
    }
}