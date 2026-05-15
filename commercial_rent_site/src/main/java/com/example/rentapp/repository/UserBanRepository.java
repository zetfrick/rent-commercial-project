package com.example.rentapp.repository;

import com.example.rentapp.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    Optional<UserBan> findByUserIdAndActiveTrue(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserBan b SET b.active = false WHERE b.userId = :userId AND b.active = true")
    void deactivateActiveBan(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserBan b WHERE b.bannedUntil < :now AND b.active = true")
    int deleteExpiredBans(@Param("now") LocalDateTime now);
}