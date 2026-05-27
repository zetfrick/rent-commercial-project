package com.example.rentapp.repository;

import com.example.rentapp.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) ORDER BY m.sentAt ASC")
    List<ChatMessage> findChatBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :user1 AND m.receiverId = :user2 AND m.premiseId = :premiseId) OR (m.senderId = :user2 AND m.receiverId = :user1 AND m.premiseId = :premiseId) ORDER BY m.sentAt ASC")
    List<ChatMessage> findChatBetweenUsersByPremise(@Param("user1") Long user1, @Param("user2") Long user2, @Param("premiseId") Long premiseId);

    @Query("SELECT m FROM ChatMessage m WHERE m.senderId = :userId OR m.receiverId = :userId ORDER BY m.sentAt DESC")
    List<ChatMessage> findAllMessagesForUser(@Param("userId") Long userId);

    @Query("SELECT m FROM ChatMessage m WHERE m.receiverId = :receiverId AND m.read = false")
    List<ChatMessage> findByReceiverIdAndReadFalse(@Param("receiverId") Long receiverId);

    @Query("SELECT m FROM ChatMessage m WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.read = false")
    List<ChatMessage> findUnreadMessagesBetweenUsers(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.read = false")
    void markMessagesAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.premiseId = :premiseId AND m.read = false")
    void markMessagesAsReadByPremise(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId, @Param("premiseId") Long premiseId);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) AND m.fileName IS NOT NULL ORDER BY m.sentAt ASC")
    List<ChatMessage> findFileMessagesBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);

    // НОВЫЕ МЕТОДЫ ДЛЯ СТАТУСА ДОСТАВКИ

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.deliveryStatus = :status WHERE m.id = :messageId")
    void updateDeliveryStatus(@Param("messageId") Long messageId, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.deliveryStatus = :status WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.deliveryStatus != 'READ'")
    void updateDeliveryStatusByUsers(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.deliveryStatus = :status WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.premiseId = :premiseId AND m.deliveryStatus != 'READ'")
    void updateDeliveryStatusByUsersAndPremise(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId, @Param("premiseId") Long premiseId, @Param("status") String status);

    @Query("SELECT m FROM ChatMessage m WHERE ((m.senderId = :user1 AND m.receiverId = :user2 AND m.premiseId = :premiseId) OR (m.senderId = :user2 AND m.receiverId = :user1 AND m.premiseId = :premiseId)) AND m.fileName IS NOT NULL ORDER BY m.sentAt ASC")
    List<ChatMessage> findFileMessagesBetweenUsersByPremise(@Param("user1") Long user1, @Param("user2") Long user2, @Param("premiseId") Long premiseId);
}