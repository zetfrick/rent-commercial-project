package com.example.rentapp.repository;

import com.example.rentapp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Найти все бронирования для помещения
    List<Booking> findByPremiseId(Long premiseId);

    // Найти бронирования по помещению и статусу
    List<Booking> findByPremiseIdAndStatus(Long premiseId, String status);

    // Найти бронирования по помещению и списку статусов
    @Query("SELECT b FROM Booking b WHERE b.premiseId = :premiseId AND b.status IN :statuses")
    List<Booking> findByPremiseIdAndStatusIn(@Param("premiseId") Long premiseId, @Param("statuses") List<String> statuses);

    // Найти PENDING запросы для владельца
    List<Booking> findByOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, String status);

    // Найти все бронирования арендатора
    List<Booking> findByRenterIdOrderByCreatedAtDesc(Long renterId);

    // Найти все бронирования владельца
    List<Booking> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    // Найти APPROVED бронирования арендатора
    List<Booking> findByRenterIdAndStatus(Long renterId, String status);

    // Найти APPROVED бронирования, пересекающиеся с заданным диапазоном дат
    @Query("SELECT b FROM Booking b WHERE b.premiseId = :premiseId AND b.status = 'APPROVED' " +
            "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findOverlappingApprovedBookings(@Param("premiseId") Long premiseId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    // Обновить статус бронирования
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.status = :status WHERE b.id = :bookingId")
    void updateStatus(@Param("bookingId") Long bookingId, @Param("status") String status);

    // НОВЫЙ МЕТОД: Найти APPROVED бронирования, которые начинаются через 3 дня, 1 день или сегодня
    @Query("SELECT b FROM Booking b WHERE b.status = 'APPROVED' AND b.startDate IN (:in3Days, :in1Day, :today)")
    List<Booking> findUpcomingApprovedBookings(@Param("in3Days") LocalDate in3Days,
                                               @Param("in1Day") LocalDate in1Day,
                                               @Param("today") LocalDate today);

    // НОВЫЙ МЕТОД: Найти APPROVED бронирования, которые заканчиваются через 3 дня, 1 день или сегодня
    @Query("SELECT b FROM Booking b WHERE b.status = 'APPROVED' AND b.endDate IN (:in3Days, :in1Day, :today)")
    List<Booking> findEndingApprovedBookings(@Param("in3Days") LocalDate in3Days,
                                             @Param("in1Day") LocalDate in1Day,
                                             @Param("today") LocalDate today);
}