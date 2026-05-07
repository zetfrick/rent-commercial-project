package com.example.rentapp.repository;

import com.example.rentapp.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read);

    int countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.id IN :ids")
    void markAsRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);  // ← ОСТАВЛЯЕМ Long

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.id IN :ids")
    void deleteByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.read = true AND n.createdAt < :date")
    int deleteOldReadNotifications(@Param("userId") Long userId, @Param("date") LocalDateTime date);
}