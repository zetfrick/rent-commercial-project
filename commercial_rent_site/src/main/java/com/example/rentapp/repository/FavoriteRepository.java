package com.example.rentapp.repository;

import com.example.rentapp.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndPremiseId(Long userId, Long premiseId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndPremiseId(Long userId, Long premiseId);

    @Modifying
    @Transactional
    void deleteByUserIdAndPremiseId(Long userId, Long premiseId);
}