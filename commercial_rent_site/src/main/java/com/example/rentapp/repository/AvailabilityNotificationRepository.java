package com.example.rentapp.repository;

import com.example.rentapp.entity.AvailabilityNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityNotificationRepository extends JpaRepository<AvailabilityNotification, Long> {

    List<AvailabilityNotification> findByPremiseIdAndNotifiedFalse(Long premiseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AvailabilityNotification n WHERE n.premiseId = :premiseId AND n.startDate = :startDate AND n.endDate = :endDate AND n.userId = :userId")
    void deleteByPremiseIdAndDatesAndUserId(Long premiseId, LocalDate startDate, LocalDate endDate, Long userId);

    boolean existsByPremiseIdAndUserIdAndStartDateAndEndDate(Long premiseId, Long userId, LocalDate startDate, LocalDate endDate);
}