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
        } else {
            complaint = new Complaint(
                    dto.getSubject(),
                    dto.getReason(),
                    dto.getUserId(),
                    dto.getComplainantId(),
                    dto.getComplainantName(),
                    dto.getTargetName(),
                    true
            );
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
        // Получаем жалобу
        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();

        // Добавляем запись об отказе
        List<RejectedBy> rejectedBy = complaint.getRejectedBy();
        if (rejectedBy == null) {
            rejectedBy = new ArrayList<>();
        }
        rejectedBy.add(new RejectedBy(adminId, adminName, LocalDateTime.now()));
        complaint.setRejectedBy(rejectedBy);

        // Меняем статус обратно на ACTIVE
        complaint.setStatus("ACTIVE");
        complaint.setResolved(false);

        // Сохраняем
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

        // Устанавливаем статус для фронтенда
        if (complaint.isResolved()) {
            dto.setStatus("RESOLVED");
        } else {
            dto.setStatus(complaint.getStatus());
        }

        // Конвертируем список отказов
        if (complaint.getRejectedBy() != null && !complaint.getRejectedBy().isEmpty()) {
            List<RejectedByDto> rejectedDtos = complaint.getRejectedBy().stream()
                    .map(r -> new RejectedByDto(r.getAdminId(), r.getAdminName(), r.getRejectedAt()))
                    .collect(Collectors.toList());
            dto.setRejectedBy(rejectedDtos);
        }

        return dto;
    }
}