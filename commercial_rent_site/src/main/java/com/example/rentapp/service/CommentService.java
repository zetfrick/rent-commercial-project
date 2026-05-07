package com.example.rentapp.service;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.CommentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    @Autowired
    private CatalogClient catalogClient;

    // Убираем notificationService и userService отсюда - уведомления будут в контроллере

    public CommentDto addComment(CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Текст комментария не может быть пустым");
        }
        if (commentDto.getPremiseId() == null) {
            throw new IllegalArgumentException("Не указан ID помещения");
        }

        // Только сохраняем комментарий через catalog-client, без уведомлений
        return catalogClient.addComment(commentDto);
    }

    public List<CommentDto> getCommentsByPremiseId(Long premiseId) {
        return catalogClient.getCommentsByPremiseId(premiseId);
    }

    // НОВЫЙ МЕТОД: удаление комментария
    public boolean deleteComment(Long commentId) {
        try {
            Map<String, Object> result = catalogClient.deleteComment(commentId);
            return result != null && Boolean.TRUE.equals(result.get("success"));
        } catch (Exception e) {
            System.err.println("Error deleting comment: " + e.getMessage());
            return false;
        }
    }
}