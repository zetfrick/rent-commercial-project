package com.example.rentapp.controller;

import com.example.rentapp.config.WebSocketConfig;
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
import java.util.Arrays;
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

            User sender = userService.findById(message.getSenderId()).orElse(null);
            User receiver = userService.findById(message.getReceiverId()).orElse(null);

            if (sender == null || receiver == null) {
                System.err.println("❌ Пользователь не найден!");
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

            // Проверяем, онлайн ли получатель
            boolean isReceiverOnline = WebSocketConfig.onlineUsers.containsKey(receiverUsername);

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
            response.setDeliveryStatus(savedMessage.getDeliveryStatus());

            // Отправляем получателю (если он онлайн)
            if (isReceiverOnline) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            receiverUsername,
                            "/queue/messages",
                            response
                    );
                    System.out.println("✅ WebSocket сообщение отправлено получателю: " + receiverUsername);

                    // После успешной отправки получателю обновляем статус доставки
                    chatService.updateMessageDeliveryStatus(savedMessage.getId(), "RECEIVED");
                    response.setDeliveryStatus("RECEIVED");

                    // Отправляем обновление статуса отправителю
                    messagingTemplate.convertAndSendToUser(
                            senderUsername,
                            "/queue/messages",
                            response
                    );

                } catch (Exception e) {
                    System.out.println("⚠️ Ошибка отправки получателю: " + e.getMessage());
                    // Если получатель не в сети, отправляем только отправителю с текущим статусом
                    messagingTemplate.convertAndSendToUser(
                            senderUsername,
                            "/queue/messages",
                            response
                    );
                }
            } else {
                System.out.println("⚠️ Получатель не в сети: " + receiverUsername);
                messagingTemplate.convertAndSendToUser(
                        senderUsername,
                        "/queue/messages",
                        response
                );
            }

            System.out.println("✅ WebSocket сообщение обработано");
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
            if (receiver != null && WebSocketConfig.onlineUsers.containsKey(receiver.getLogin())) {
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
            // ВАЖНО: currentUserId - это получатель (кто прочитал)
            // otherUserId - это отправитель (чей сообщения прочитаны)
            Long currentUserId = message.getReceiverId();  // кто прочитал
            Long otherUserId = message.getSenderId();      // чьи сообщения

            System.out.println("=== ОТМЕТКА ПРОЧИТАННЫХ ===");
            System.out.println("Пользователь ID " + currentUserId + " прочитал сообщения от ID " + otherUserId);
            System.out.println("ID помещения: " + message.getPremiseId());

            if (message.getPremiseId() != null && message.getPremiseId() > 0) {
                chatService.markMessagesAsReadByPremise(
                        currentUserId,
                        otherUserId,
                        message.getPremiseId()
                );
            } else {
                chatService.markMessagesAsRead(
                        currentUserId,
                        otherUserId
                );
            }

            // ОТПРАВЛЯЕМ ПОДТВЕРЖДЕНИЕ ПРОЧТЕНИЯ ОТПРАВИТЕЛЮ
            User sender = userService.findById(otherUserId).orElse(null);
            if (sender != null && WebSocketConfig.onlineUsers.containsKey(sender.getLogin())) {
                WebSocketMessageDto readReceipt = new WebSocketMessageDto();
                readReceipt.setId(message.getId());
                readReceipt.setSenderId(currentUserId);
                readReceipt.setReceiverId(otherUserId);
                readReceipt.setType("READ_RECEIPT");
                readReceipt.setPremiseId(message.getPremiseId());
                readReceipt.setDeliveryStatus("READ");

                messagingTemplate.convertAndSendToUser(
                        sender.getLogin(),
                        "/queue/read",
                        readReceipt
                );
                System.out.println("✅ Подтверждение прочтения отправлено пользователю " + sender.getLogin());
            }

            // Также отправляем обновление получателю (для синхронизации)
            User receiver = userService.findById(currentUserId).orElse(null);
            if (receiver != null && WebSocketConfig.onlineUsers.containsKey(receiver.getLogin())) {
                WebSocketMessageDto receipt = new WebSocketMessageDto();
                receipt.setId(message.getId());
                receipt.setSenderId(otherUserId);
                receipt.setReceiverId(currentUserId);
                receipt.setType("READ_RECEIPT");
                receipt.setPremiseId(message.getPremiseId());
                receipt.setDeliveryStatus("READ");

                messagingTemplate.convertAndSendToUser(
                        receiver.getLogin(),
                        "/queue/read",
                        receipt
                );
            }

        } catch (Exception e) {
            System.err.println("Error marking messages as read: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.delivered")
    public void markAsDelivered(@Payload WebSocketMessageDto message) {
        try {
            // Обновляем статус доставки для сообщений отправителя
            if (message.getPremiseId() != null && message.getPremiseId() > 0) {
                chatService.updateMessagesStatusBetweenUsers(
                        message.getSenderId(),
                        message.getReceiverId(),
                        message.getPremiseId(),
                        "DELIVERED"
                );
            } else {
                chatService.updateMessagesStatusBetweenUsers(
                        message.getSenderId(),
                        message.getReceiverId(),
                        null,
                        "DELIVERED"
                );
            }

            User receiver = userService.findById(message.getReceiverId()).orElse(null);
            if (receiver != null && WebSocketConfig.onlineUsers.containsKey(receiver.getLogin())) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getLogin(),
                        "/queue/delivered",
                        message
                );
            }
        } catch (Exception e) {
            System.err.println("Error marking messages as delivered: " + e.getMessage());
        }
    }

    // НОВЫЙ ЭНДПОИНТ: отслеживание подключения пользователя
    @MessageMapping("/chat.connect")
    public void connect(Principal principal) {
        if (principal != null && principal.getName() != null) {
            WebSocketConfig.onlineUsers.put(principal.getName(), "connected");
            System.out.println("🔵 Пользователь подключился: " + principal.getName());
            System.out.println("Онлайн пользователей: " + WebSocketConfig.onlineUsers.size());
        }
    }

    // НОВЫЙ ЭНДПОИНТ: отслеживание отключения пользователя
    @MessageMapping("/chat.disconnect")
    public void disconnect(Principal principal) {
        if (principal != null && principal.getName() != null) {
            WebSocketConfig.onlineUsers.remove(principal.getName());
            System.out.println("🔴 Пользователь отключился: " + principal.getName());
            System.out.println("Онлайн пользователей: " + WebSocketConfig.onlineUsers.size());
        }
    }

    @PostMapping("/chats/send-file")
    @ResponseBody
    public Map<String, Object> sendFile(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long premiseId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();

        // 1. Проверка авторизации
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

        // 2. Валидация файла
        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Файл не выбран");
            return response;
        }

        long maxSize = 10 * 1024 * 1024; // 10 MB
        if (file.getSize() > maxSize) {
            response.put("success", false);
            response.put("message", "Файл слишком большой (макс. 10 МБ)");
            return response;
        }

        // 3. Проверка типа файла
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain",
                "application/zip", "application/x-rar-compressed"
        );

        if (!allowedTypes.contains(contentType) && !contentType.startsWith("image/")) {
            response.put("success", false);
            response.put("message", "Тип файла не поддерживается");
            return response;
        }

        try {
            // 4. Сохраняем файл на диск
            String fileName = fileStorageService.saveChatFile(file);
            String fileUrl = "/chat-uploads/" + fileName;
            String fileHtml = generateFileHtml(fileName, fileUrl, file.getContentType(), file.getSize());

            // 5. Сохраняем сообщение в БД
            ChatMessage savedMessage;
            if (premiseId != null && premiseId > 0) {
                savedMessage = chatService.sendMessageWithFile(
                        sender.getId(),
                        receiverId,
                        fileHtml,
                        premiseId,
                        fileName,
                        fileUrl,
                        file.getContentType(),
                        file.getSize()
                );
            } else {
                savedMessage = chatService.sendMessageWithFile(
                        sender.getId(),
                        receiverId,
                        fileHtml,
                        null,
                        fileName,
                        fileUrl,
                        file.getContentType(),
                        file.getSize()
                );
            }

            // 6. Получаем информацию о получателе
            User receiver = userService.findById(receiverId).orElse(null);

            // 7. Подготавливаем WebSocket сообщение
            WebSocketMessageDto wsMessage = new WebSocketMessageDto(
                    savedMessage.getId(),
                    savedMessage.getSenderId(),
                    savedMessage.getReceiverId(),
                    savedMessage.getSenderLogin(),
                    savedMessage.getText(),
                    savedMessage.getSentAt(),
                    savedMessage.getPremiseId(),
                    "FILE",
                    fileName,
                    fileUrl,
                    file.getContentType(),
                    file.getSize()
            );

            // 8. Проверяем, онлайн ли получатель
            boolean isReceiverOnline = receiver != null && WebSocketConfig.onlineUsers.containsKey(receiver.getLogin());

            if (isReceiverOnline && receiver != null) {
                // Получатель онлайн - отправляем через WebSocket
                try {
                    messagingTemplate.convertAndSendToUser(
                            receiver.getLogin(),
                            "/queue/messages",
                            wsMessage
                    );

                    // Обновляем статус доставки на RECEIVED
                    chatService.updateMessageDeliveryStatus(savedMessage.getId(), "RECEIVED");
                    wsMessage.setDeliveryStatus("RECEIVED");

                    System.out.println("✅ Файл доставлен онлайн-получателю: " + receiver.getLogin());

                } catch (Exception e) {
                    System.err.println("WebSocket send error to receiver: " + e.getMessage());
                    wsMessage.setDeliveryStatus("DELIVERED");
                }
            } else {
                // Получатель оффлайн - оставляем статус DELIVERED
                System.out.println("📁 Получатель оффлайн: " + (receiver != null ? receiver.getLogin() : "unknown") +
                        ", файл сохранён в БД, будет доставлен при входе");
                wsMessage.setDeliveryStatus("DELIVERED");
            }

            // 9. ВАЖНО: ВСЕГДА отправляем ФИНАЛЬНОЕ подтверждение отправителю
            // Это заменит временное сообщение на постоянное с правильным статусом
            try {
                messagingTemplate.convertAndSendToUser(
                        sender.getLogin(),
                        "/queue/messages",
                        wsMessage
                );
                System.out.println("✅ Подтверждение отправлено отправителю: " + sender.getLogin() +
                        " (статус: " + wsMessage.getDeliveryStatus() + ")");
            } catch (Exception e) {
                System.err.println("WebSocket send error to sender: " + e.getMessage());
            }

            // 10. Отправляем уведомление (если получатель есть)
            if (receiver != null) {
                try {
                    String chatLink = (premiseId != null && premiseId > 0)
                            ? "/chats/with/" + sender.getLogin() + "/premise/" + premiseId
                            : "/chats/with/" + sender.getLogin();

                    if (!isReceiverOnline || !notificationService.hasUnreadMessageNotification(receiverId, sender.getId(), premiseId)) {
                        notificationService.createNotification(
                                receiverId,
                                "MESSAGE",
                                premiseId,
                                sender.getId(),
                                sender.getLogin(),
                                "📎 Отправлен файл: " + fileName,
                                chatLink
                        );
                        System.out.println("🔔 Уведомление о файле отправлено получателю");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to send notification: " + e.getMessage());
                }
            }

            // 11. Формируем успешный ответ
            response.put("success", true);
            response.put("messageId", savedMessage.getId());
            response.put("fileHtml", fileHtml);
            response.put("fileName", fileName);
            response.put("fileUrl", fileUrl);
            response.put("deliveryStatus", wsMessage.getDeliveryStatus());

            if (!isReceiverOnline && receiver != null) {
                response.put("message", "Файл отправлен. Получатель увидит его при следующем входе.");
            } else if (isReceiverOnline) {
                response.put("message", "Файл успешно отправлен и доставлен");
            }

        } catch (Exception e) {
            System.err.println("Error saving file: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка сохранения файла: " + e.getMessage());
        }

        return response;
    }

    private String generateFileHtml(String fileName, String fileUrl, String contentType, long fileSize) {
        String sizeStr = formatFileSize(fileSize);
        String fileExtension = getFileExtension(fileName).toLowerCase();

        String iconHtml;
        String previewHtml = "";

        if (contentType != null && contentType.startsWith("image/")) {
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