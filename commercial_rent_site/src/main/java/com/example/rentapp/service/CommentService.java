package com.example.rentapp.service;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.CommentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<CommentDto> getCommentsByPremiseId(Long premiseId) {
        return catalogClient.getCommentsByPremiseId(premiseId);
    }
}