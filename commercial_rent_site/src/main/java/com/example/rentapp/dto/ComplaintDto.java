package com.example.rentapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDto {

    private Long id;
    private String subject;
    private String reason;
    private String type;
    private Long premiseId;
    private Long userId;
    private Long commentId;        // НОВОЕ ПОЛЕ: ID комментария
    private Long complainantId;
    private String complainantName;
    private String targetName;
    private boolean resolved;
    private String status;
    private Long resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private List<RejectedByDto> rejectedBy;

    // НОВОЕ ПОЛЕ: детали комментария для отображения в админке
    private CommentDetails commentDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedByDto {
        private Long adminId;
        private String adminName;
        private LocalDateTime rejectedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDetails {
        private Long id;
        private String authorName;
        private String text;
        private LocalDateTime createdAt;
    }
}