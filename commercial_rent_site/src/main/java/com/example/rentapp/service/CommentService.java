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

    public CommentDto addComment(CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Текст комментария не может быть пустым");
        }
        if (commentDto.getPremiseId() == null) {
            throw new IllegalArgumentException("Не указан ID помещения");
        }
        return catalogClient.addComment(commentDto);
    }

    // Добавление ответа на комментарий
    public CommentDto addReply(CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Текст ответа не может быть пустым");
        }
        if (commentDto.getPremiseId() == null) {
            throw new IllegalArgumentException("Не указан ID помещения");
        }
        if (commentDto.getParentCommentId() == null) {
            throw new IllegalArgumentException("Не указан ID комментария, на который отвечаете");
        }
        return catalogClient.addReply(commentDto);
    }

    //Получение комментариев с ответами (иерархически)
    public List<CommentDto> getCommentsWithReplies(Long premiseId) {
        return catalogClient.getCommentsWithReplies(premiseId);
    }

    // Получение плоского списка комментариев (без иерархии) - для обратной совместимости
    public List<CommentDto> getCommentsByPremiseId(Long premiseId) {
        return catalogClient.getCommentsByPremiseId(premiseId);
    }

    // Удаление комментария
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