package com.example.rentapp.service;

import com.example.rentapp.dto.FavoriteDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.Favorite;
import com.example.rentapp.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional
    public boolean toggleFavorite(Long userId, Long premiseId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndPremiseId(userId, premiseId);

        if (existing.isPresent()) {
            favoriteRepository.deleteByUserIdAndPremiseId(userId, premiseId);
            return false; // удалено
        } else {
            Favorite favorite = new Favorite(userId, premiseId);
            favoriteRepository.save(favorite);
            return true; // добавлено
        }
    }

    public boolean isFavorite(Long userId, Long premiseId) {
        return favoriteRepository.existsByUserIdAndPremiseId(userId, premiseId);
    }

    public List<Long> getFavoritePremiseIds(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(Favorite::getPremiseId)
                .collect(Collectors.toList());
    }

    public List<FavoriteDto> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(f -> new FavoriteDto(f.getId(), f.getUserId(), f.getPremiseId(), f.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFavorite(Long userId, Long premiseId) {
        favoriteRepository.deleteByUserIdAndPremiseId(userId, premiseId);
    }
}