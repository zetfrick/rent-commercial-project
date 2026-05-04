package com.example.rentapp.service;

import com.example.rentapp.dto.ComplaintDto;
import com.example.rentapp.dto.ComplaintDto.RejectedByDto;
import com.example.rentapp.entity.Complaint;
import com.example.rentapp.entity.Complaint.RejectedBy;
import com.example.rentapp.repository.ComplaintRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    public ComplaintDto saveComplaint(ComplaintDto dto) {
        Complaint complaint;

        if ("PREMISE".equals(dto.getType())) {
            complaint = new Complaint(
                    dto.getSubject(),
                    dto.getReason(),
                    dto.getPremiseId(),
                    dto.getComplainantId(),
                    dto.getComplainantName(),
                    dto.getTargetName()
            );
        } else if ("USER".equals(dto.getType())) {
            complaint = new Complaint(
                    dto.getSubject(),
                    dto.getReason(),
                    dto.getUserId(),
                    dto.getComplainantId(),
                    dto.getComplainantName(),
                    dto.getTargetName(),
                    true
            );
        } else {  // "COMMENT"
            complaint = new Complaint(
                    dto.getSubject(),
                    dto.getReason(),
                    dto.getCommentId(),
                    dto.getPremiseId(),
                    dto.getComplainantId(),
                    dto.getComplainantName(),
                    dto.getTargetName()
            );
            // Сохраняем текст комментария и имя автора прямо в жалобу
            if (dto.getCommentDetails() != null) {
                complaint.setCommentText(dto.getCommentDetails().getText());
                complaint.setCommentAuthor(dto.getCommentDetails().getAuthorName());
            } else {
                // Извлекаем имя автора из targetName (формат: "Комментарий от admin: текст")
                String targetName = dto.getTargetName();
                if (targetName != null && targetName.startsWith("Комментарий от ")) {
                    int authorEndIndex = targetName.indexOf(":", 15);
                    if (authorEndIndex > 0) {
                        String author = targetName.substring(15, authorEndIndex).trim();
                        complaint.setCommentAuthor(author);
                    }
                    String text = targetName.substring(targetName.indexOf(":") + 1).trim();
                    complaint.setCommentText(text);
                }
            }
        }

        Complaint saved = complaintRepository.save(complaint);
        return convertToDto(saved);
    }

    public List<ComplaintDto> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getActiveComplaints() {
        return complaintRepository.findByStatusAndResolvedOrderByCreatedAtDesc("ACTIVE", false)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getInWorkComplaints() {
        return complaintRepository.findByStatusOrderByCreatedAtDesc("IN_WORK")
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getResolvedComplaints() {
        return complaintRepository.findByResolvedOrderByCreatedAtDesc(true)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ComplaintDto getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        return complaint != null ? convertToDto(complaint) : null;
    }

    @Transactional
    public void resolveComplaint(Long complaintId, Long adminId, String adminName) {
        complaintRepository.resolveComplaint(complaintId, adminId, adminName);
    }

    @Transactional
    public void takeWorkComplaint(Long complaintId) {
        complaintRepository.takeWorkComplaint(complaintId);
    }

    @Transactional
    public void rejectComplaint(Long complaintId, Long adminId, String adminName) {
        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();

        List<RejectedBy> rejectedBy = complaint.getRejectedBy();
        if (rejectedBy == null) {
            rejectedBy = new ArrayList<>();
        }
        rejectedBy.add(new RejectedBy(adminId, adminName, LocalDateTime.now()));
        complaint.setRejectedBy(rejectedBy);

        complaint.setStatus("ACTIVE");
        complaint.setResolved(false);

        complaintRepository.save(complaint);
    }

    @Transactional
    public int deleteOldResolvedComplaints(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Complaint> oldComplaints = complaintRepository.findByResolvedTrueAndResolvedAtBefore(cutoffDate);
        int count = oldComplaints.size();
        if (count > 0) {
            complaintRepository.deleteAll(oldComplaints);
        }
        return count;
    }

    private ComplaintDto convertToDto(Complaint complaint) {
        ComplaintDto dto = new ComplaintDto();
        BeanUtils.copyProperties(complaint, dto);

        if (complaint.isResolved()) {
            dto.setStatus("RESOLVED");
        } else {
            dto.setStatus(complaint.getStatus());
        }

        if (complaint.getRejectedBy() != null && !complaint.getRejectedBy().isEmpty()) {
            List<RejectedByDto> rejectedDtos = complaint.getRejectedBy().stream()
                    .map(r -> new RejectedByDto(r.getAdminId(), r.getAdminName(), r.getRejectedAt()))
                    .collect(Collectors.toList());
            dto.setRejectedBy(rejectedDtos);
        }

        // Для жалоб на комментарии используем сохранённые данные
        if ("COMMENT".equals(complaint.getType())) {
            ComplaintDto.CommentDetails details = new ComplaintDto.CommentDetails();
            details.setId(complaint.getCommentId() != null ? complaint.getCommentId() : 0L);
            details.setAuthorName(complaint.getCommentAuthor() != null ? complaint.getCommentAuthor() : "неизвестен");
            details.setText(complaint.getCommentText() != null ? complaint.getCommentText() : "Текст комментария не сохранён");
            details.setCreatedAt(complaint.getCreatedAt());
            dto.setCommentDetails(details);
        }

        return dto;
    }
}