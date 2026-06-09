package com.example.rentapp.service;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CatalogServiceWrapper {

    @Autowired(required = false)
    private CatalogClient catalogClient;

    // Флаг доступности сервиса
    private boolean serviceAvailable = true;
    private long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 5000; // проверка раз в 5 секунд

    public boolean isServiceAvailable() {
        // Кешируем результат на 5 секунд
        if (System.currentTimeMillis() - lastCheckTime < CHECK_INTERVAL) {
            return serviceAvailable;
        }

        try {
            // Пытаемся получить конфигурацию (лёгкий запрос)
            catalogClient.getAllConfig();
            serviceAvailable = true;
        } catch (Exception e) {
            serviceAvailable = false;
            System.err.println("⚠️ CatalogService недоступен");
        }
        lastCheckTime = System.currentTimeMillis();
        return serviceAvailable;
    }

    private PremiseDto createPlaceholderPremise(Long premiseId) {
        PremiseDto placeholder = new PremiseDto();
        placeholder.setId(premiseId);
        placeholder.setType("СЕРВИС НЕДОСТУПЕН");
        placeholder.setCity("ВЕДУТСЯ РАБОТЫ");
        placeholder.setStreet("");
        placeholder.setBuilding("");
        placeholder.setActive(false);
        placeholder.setAvailableFrom(LocalDate.now());
        placeholder.setAvailableTo(LocalDate.now().plusYears(1));
        placeholder.setPriceWeekday(0);
        placeholder.setPhotoPaths(new ArrayList<>());
        placeholder.setAmenities(new ArrayList<>());
        placeholder.setContactFirstName("ПОЧТА");
        placeholder.setContactLastName("ПОДДЕРЖКИ: support@rentapp.ru");
        placeholder.setContactPhone("support@rentapp.ru");
        return placeholder;
    }

    public PremiseDto getPremiseById(Long id) {
        try {
            return catalogClient.getPremiseById(id);
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем плейсхолдер для помещения #" + id);
            return createPlaceholderPremise(id);
        }
    }

    public List<PremiseDto> getPremisesByOwnerId(Long ownerId) {
        try {
            return catalogClient.getPremisesByOwnerId(ownerId);
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем пустой список для владельца #" + ownerId);
            return new ArrayList<>();
        }
    }

    public List<PremiseDto> getLatestPremises(int limit) {
        try {
            return catalogClient.getLatestPremises(limit);
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем пустой список последних помещений");
            return new ArrayList<>();
        }
    }

    public List<PremiseDto> getAllPremises() {
        try {
            return catalogClient.getAllPremises();
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем пустой список");
            return new ArrayList<>();
        }
    }

    public PremiseDto addPremise(PremiseDto premiseDto) {
        return catalogClient.addPremise(premiseDto);
    }

    public PremiseDto updatePremise(Long id, PremiseDto premiseDto) {
        return catalogClient.updatePremise(id, premiseDto);
    }

    public CommentDto addComment(CommentDto commentDto) {
        return catalogClient.addComment(commentDto);
    }

    public List<CommentDto> getCommentsByPremiseId(Long premiseId) {
        try {
            return catalogClient.getCommentsByPremiseId(premiseId);
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем пустой список комментариев");
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getAllConfig() {
        return catalogClient.getAllConfig();
    }

    public Map<String, Object> togglePublish(Long id, Map<String, Boolean> request) {
        return catalogClient.togglePublish(id, request);
    }

    public Map<String, Object> deletePremise(Long id) {
        return catalogClient.deletePremise(id);
    }

    public Map<String, Object> deleteComment(Long id) {
        return catalogClient.deleteComment(id);
    }

    public void updateOwnerContacts(Long ownerId, Map<String, String> contacts) {
        catalogClient.updateOwnerContacts(ownerId, contacts);
    }

    public List<CommentDto> getCommentsWithReplies(Long premiseId) {
        try {
            return catalogClient.getCommentsWithReplies(premiseId);
        } catch (Exception e) {
            System.err.println("⚠️ CatalogService недоступен, возвращаем пустой список комментариев с ответами");
            return new ArrayList<>();
        }
    }

    public CommentDto addReply(CommentDto commentDto) {
        return catalogClient.addReply(commentDto);
    }
}