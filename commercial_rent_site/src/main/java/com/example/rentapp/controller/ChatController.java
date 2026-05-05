package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.ChatMessageDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.ChatMessage;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.BookingService;
import com.example.rentapp.service.ChatService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chats")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String chatsPage(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(required = false) String city,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        model.addAttribute("currentCity", city != null ? city : "Нижний Новгород");
        model.addAttribute("currentUri", request.getRequestURI());

        List<ChatMessage> allMessages = chatService.getAllUserMessages(currentUser.getId());

        Map<String, ChatInfo> chatMap = new LinkedHashMap<>();

        for (ChatMessage msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(currentUser.getId()) ? msg.getReceiverId() : msg.getSenderId();
            Long premiseIdObj = msg.getPremiseId();
            String key = otherUserId + "_" + (premiseIdObj != null ? premiseIdObj : 0);

            if (!chatMap.containsKey(key)) {
                User otherUser = userService.findById(otherUserId).orElse(null);
                if (otherUser != null) {
                    ChatInfo info = new ChatInfo();
                    info.user = otherUser;
                    info.premiseId = premiseIdObj;
                    info.lastMessage = msg.getText();
                    info.lastMessageTime = msg.getSentAt();
                    info.unreadCount = (msg.getReceiverId().equals(currentUser.getId()) && !msg.isRead()) ? 1 : 0;

                    if (premiseIdObj != null) {
                        try {
                            info.premise = catalogClient.getPremiseById(premiseIdObj);
                        } catch (Exception e) {
                            System.err.println("Error loading premise: " + e.getMessage());
                        }
                    }
                    chatMap.put(key, info);
                }
            } else {
                ChatInfo info = chatMap.get(key);
                if (msg.getReceiverId().equals(currentUser.getId()) && !msg.isRead()) {
                    info.unreadCount++;
                }
                if (msg.getSentAt().isAfter(info.lastMessageTime)) {
                    info.lastMessage = msg.getText();
                    info.lastMessageTime = msg.getSentAt();
                }
            }
        }

        List<ChatInfo> chats = new ArrayList<>(chatMap.values());
        chats.sort((a, b) -> b.lastMessageTime.compareTo(a.lastMessageTime));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chats", chats);

        return "future/chats";
    }

    @GetMapping("/with/{username}")
    public String chatWithUser(@PathVariable String username,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        User otherUser = userService.findByLogin(username).orElseThrow();

        chatService.markMessagesAsRead(currentUser.getId(), otherUser.getId());

        List<ChatMessage> messages = chatService.getChatBetween(currentUser.getId(), otherUser.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("messages", messages);
        model.addAttribute("otherUsername", username);

        return "future/chat-window";
    }

    @GetMapping("/with/{username}/premise/{premiseId}")
    public String chatWithUserByPremise(@PathVariable String username,
                                        @PathVariable Long premiseId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        Model model) {

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        User otherUser = userService.findByLogin(username).orElseThrow();

        PremiseDto premise = catalogClient.getPremiseById(premiseId);

        chatService.markMessagesAsReadByPremise(currentUser.getId(), otherUser.getId(), premiseId);

        List<ChatMessage> messages = chatService.getChatBetweenByPremise(currentUser.getId(), otherUser.getId(), premiseId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("messages", messages);
        model.addAttribute("otherUsername", username);
        model.addAttribute("premiseId", premiseId);
        model.addAttribute("premise", premise);
        model.addAttribute("isOwner", currentUser.getId().equals(premise.getOwnerId()));

        return "future/chat-window";
    }

    @GetMapping("/premise/{premiseId}")
    public String chatWithPremiseOwner(@PathVariable Long premiseId,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       Model model) {

        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        PremiseDto premise = catalogClient.getPremiseById(premiseId);

        if (premise == null) {
            return "redirect:/catalog?error=premise_not_found";
        }

        User owner = userService.findById(premise.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Владелец помещения не найден"));

        chatService.markMessagesAsReadByPremise(currentUser.getId(), owner.getId(), premiseId);

        List<ChatMessage> messages = chatService.getChatBetweenByPremise(currentUser.getId(), owner.getId(), premiseId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", owner);
        model.addAttribute("messages", messages);
        model.addAttribute("otherUsername", owner.getLogin());
        model.addAttribute("premiseId", premiseId);
        model.addAttribute("premiseTitle", premise.getTypeInRussian() + " в " + premise.getCity());
        model.addAttribute("premise", premise);
        model.addAttribute("isOwner", currentUser.getId().equals(premise.getOwnerId()));

        return "future/chat-window";
    }

    @PostMapping("/send")
    @ResponseBody
    public String sendMessage(@RequestParam Long receiverId,
                              @RequestParam String text,
                              @RequestParam(required = false) Long premiseId,
                              @AuthenticationPrincipal UserDetails userDetails) {

        User sender = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        System.out.println("=== SEND MESSAGE ===");
        System.out.println("Sender: " + sender.getLogin() + " (ID: " + sender.getId() + ")");
        System.out.println("Receiver ID: " + receiverId);
        System.out.println("Text: " + text);
        System.out.println("Premise ID: " + premiseId);

        ChatMessage message;
        if (premiseId != null) {
            message = chatService.sendMessageWithPremise(sender.getId(), receiverId, text, premiseId);
        } else {
            message = chatService.sendMessage(sender.getId(), receiverId, text);
        }

        System.out.println("Message saved with ID: " + message.getId() + ", premiseId: " + message.getPremiseId());

        return "ok";
    }

    // НОВЫЙ МЕТОД: отправка системного сообщения о запросе аренды
    @PostMapping("/send-system-message")
    @ResponseBody
    public String sendSystemMessage(@RequestParam Long receiverId,
                                    @RequestParam String messageType,
                                    @RequestParam(required = false) Long premiseId,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @AuthenticationPrincipal UserDetails userDetails) {

        User sender = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        String text;
        if ("booking_request".equals(messageType)) {
            String formattedStart = startDate != null ? startDate.replace('-', '.') : "";
            String formattedEnd = endDate != null ? endDate.replace('-', '.') : "";
            text = String.format("📅 <strong>Запрос на аренду</strong><br>Пользователь <strong>%s</strong> предлагает арендовать помещение с <strong>%s</strong> по <strong>%s</strong>. Проверьте запрос в разделе \"Запросы на аренду\" справа.",
                    sender.getLogin(), formattedStart, formattedEnd);
        } else if ("booking_approved".equals(messageType)) {
            text = String.format("✅ <strong>Аренда подтверждена!</strong><br>Владелец <strong>%s</strong> подтвердил аренду помещения на выбранные даты.", sender.getLogin());
        } else if ("booking_rejected".equals(messageType)) {
            text = String.format("❌ <strong>Запрос на аренду отклонён</strong><br>Владелец <strong>%s</strong> отклонил ваш запрос на аренду.", sender.getLogin());
        } else {
            text = "Системное сообщение";
        }

        ChatMessage message;
        if (premiseId != null) {
            message = chatService.sendSystemMessageWithPremise(sender.getId(), receiverId, text, premiseId);
        } else {
            message = chatService.sendSystemMessage(sender.getId(), receiverId, text);
        }

        return "ok";
    }

    @GetMapping("/api/messages")
    @ResponseBody
    public List<ChatMessageDto> getMessages(@RequestParam Long with,
                                            @RequestParam(required = false) Long premiseId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        List<ChatMessage> messages;
        if (premiseId != null) {
            messages = chatService.getChatBetweenByPremise(currentUser.getId(), with, premiseId);
        } else {
            messages = chatService.getChatBetween(currentUser.getId(), with);
        }

        return messages.stream().map(msg -> new ChatMessageDto(
                msg.getId(),
                msg.getSenderId(),
                msg.getReceiverId(),
                msg.getSenderLogin(),
                msg.getText(),
                msg.getSentAt(),
                msg.isRead()
        )).collect(Collectors.toList());
    }

    @PostMapping("/api/messages/mark-read")
    @ResponseBody
    public String markMessagesAsRead(@RequestParam Long with,
                                     @RequestParam(required = false) Long premiseId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        System.out.println("=== MARK MESSAGES AS READ ===");
        System.out.println("Current user: " + currentUser.getLogin() + " (ID: " + currentUser.getId() + ")");
        System.out.println("Other user ID: " + with);
        System.out.println("Premise ID: " + premiseId);

        if (premiseId != null) {
            chatService.markMessagesAsReadByPremise(currentUser.getId(), with, premiseId);
        } else {
            chatService.markMessagesAsRead(currentUser.getId(), with);
        }

        return "ok";
    }

    @GetMapping("/api/chats")
    @ResponseBody
    public List<ChatInfo> getChatsApi(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();

        List<ChatMessage> allMessages = chatService.getAllUserMessages(currentUser.getId());

        Map<String, ChatInfo> chatMap = new LinkedHashMap<>();

        for (ChatMessage msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(currentUser.getId()) ? msg.getReceiverId() : msg.getSenderId();
            Long premiseIdObj = msg.getPremiseId();
            String key = otherUserId + "_" + (premiseIdObj != null ? premiseIdObj : 0);

            if (!chatMap.containsKey(key)) {
                User otherUser = userService.findById(otherUserId).orElse(null);
                if (otherUser != null) {
                    ChatInfo info = new ChatInfo();
                    info.user = otherUser;
                    info.premiseId = premiseIdObj;
                    info.lastMessage = msg.getText();
                    info.lastMessageTime = msg.getSentAt();
                    info.unreadCount = (msg.getReceiverId().equals(currentUser.getId()) && !msg.isRead()) ? 1 : 0;

                    if (premiseIdObj != null) {
                        try {
                            info.premise = catalogClient.getPremiseById(premiseIdObj);
                        } catch (Exception e) {
                            System.err.println("Error loading premise: " + e.getMessage());
                        }
                    }
                    chatMap.put(key, info);
                }
            } else {
                ChatInfo info = chatMap.get(key);
                if (msg.getReceiverId().equals(currentUser.getId()) && !msg.isRead()) {
                    info.unreadCount++;
                }
                if (msg.getSentAt().isAfter(info.lastMessageTime)) {
                    info.lastMessage = msg.getText();
                    info.lastMessageTime = msg.getSentAt();
                }
            }
        }

        List<ChatInfo> chats = new ArrayList<>(chatMap.values());
        chats.sort((a, b) -> b.lastMessageTime.compareTo(a.lastMessageTime));

        return chats;
    }

    @GetMapping("/api/requests/pending")
    @ResponseBody
    public List<BookingDto> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByLogin(userDetails.getUsername()).orElseThrow();
        return bookingService.getPendingRequestsForOwner(currentUser.getId());
    }

    public static class ChatInfo {
        public User user;
        public Long premiseId;
        public PremiseDto premise;
        public String lastMessage;
        public LocalDateTime lastMessageTime;
        public int unreadCount;
    }
}