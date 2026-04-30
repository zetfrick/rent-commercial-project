package com.example.catalog.repository;

import com.example.catalog.entity.Premise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PremiseRepository extends JpaRepository<Premise, Long> {

    List<Premise> findByOwnerId(Long ownerId);

    List<Premise> findByActiveTrue();

    List<Premise> findAllByActiveTrueOrderByCreatedAtDesc();

    List<Premise> findTop6ByActiveTrueOrderByCreatedAtDesc();

    // НОВЫЕ МЕТОДЫ
    List<Premise> findByActiveTrueAndAvailableToBefore(LocalDate date);

    List<Premise> findByActiveFalseAndUnpublishedAtBefore(LocalDateTime date);
}