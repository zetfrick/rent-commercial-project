package com.example.rentapp.controller;

import com.example.rentapp.dto.ComplaintDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.ComplaintService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserService userService;

    @GetMapping("/complaints")
    public String complaintsPage(Model model) {
        return "admin/complaints";
    }

    @GetMapping("/api/complaints")
    @ResponseBody
    public List<ComplaintDto> getAllComplaints(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long adminId,
                                               @RequestParam(required = false) String adminFilter,
                                               @RequestParam(required = false) String username) {
        List<ComplaintDto> complaints;

        if ("active".equals(status)) {
            complaints = complaintService.getActiveComplaints();
        } else if ("in_work".equals(status)) {
            complaints = complaintService.getInWorkComplaints();
        } else if ("resolved".equals(status)) {
            complaints = complaintService.getResolvedComplaints();
        } else {
            complaints = complaintService.getAllComplaints();
        }

        // Фильтрация по логину жалобщика
        if (username != null && !username.isEmpty()) {
            complaints = complaints.stream()
                    .filter(c -> c.getComplainantName() != null &&
                            c.getComplainantName().toLowerCase().contains(username.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Фильтрация по администратору
        if (adminId != null) {
            if ("resolved".equals(adminFilter)) {
                complaints = complaints.stream()
                        .filter(c -> c.getResolvedBy() != null && c.getResolvedBy().equals(adminId))
                        .collect(Collectors.toList());
            } else if ("rejected".equals(adminFilter)) {
                complaints = complaints.stream()
                        .filter(c -> c.getRejectedBy() != null &&
                                c.getRejectedBy().stream().anyMatch(r -> r.getAdminId() != null && r.getAdminId().equals(adminId)))
                        .collect(Collectors.toList());
            } else {
                // Все жалобы, связанные с админом (решённые или отклонённые)
                complaints = complaints.stream()
                        .filter(c -> (c.getResolvedBy() != null && c.getResolvedBy().equals(adminId)) ||
                                (c.getRejectedBy() != null && c.getRejectedBy().stream().anyMatch(r -> r.getAdminId() != null && r.getAdminId().equals(adminId))))
                        .collect(Collectors.toList());
            }
        }

        return complaints;
    }

    @GetMapping("/api/complaints/{id}")
    @ResponseBody
    public ComplaintDto getComplaint(@PathVariable Long id) {
        return complaintService.getComplaintById(id);
    }

    // Статистика администратора
    @GetMapping("/api/complaints/stats/{adminId}")
    @ResponseBody
    public Map<String, Object> getAdminStats(@PathVariable Long adminId) {
        Map<String, Object> stats = new HashMap<>();

        List<ComplaintDto> allComplaints = complaintService.getAllComplaints();

        // Все жалобы, решённые админом
        long resolved = allComplaints.stream()
                .filter(c -> c.isResolved() && c.getResolvedBy() != null && c.getResolvedBy().equals(adminId))
                .count();

        // Все жалобы, от которых админ отказался
        long rejected = allComplaints.stream()
                .filter(c -> c.getRejectedBy() != null &&
                        c.getRejectedBy().stream().anyMatch(r -> r.getAdminId() != null && r.getAdminId().equals(adminId)))
                .count();

        // Жалобы, которые были решены после того, как от них отказывались (этот админ отказался, потом кто-то решил)
        long resolvedAfterRejection = allComplaints.stream()
                .filter(c -> c.isResolved() && c.getRejectedBy() != null &&
                        c.getRejectedBy().stream().anyMatch(r -> r.getAdminId() != null && r.getAdminId().equals(adminId)))
                .count();

        // Жалобы, которые сейчас в работе у этого админа
        long inWork = allComplaints.stream()
                .filter(c -> "IN_WORK".equals(c.getStatus()) && !c.isResolved())
                .count();

        stats.put("resolved", resolved);
        stats.put("rejected", rejected);
        stats.put("resolvedAfterRejection", resolvedAfterRejection);
        stats.put("inWork", inWork);

        return stats;
    }

    @PostMapping("/api/complaints/{id}/take-work")
    @ResponseBody
    public Map<String, Object> takeWorkComplaint(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            complaintService.takeWorkComplaint(id);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/api/complaints/{id}/reject")
    @ResponseBody
    public Map<String, Object> rejectComplaint(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return response;
        }

        User admin = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (admin == null || (admin.getRole() != User.Role.ADMIN && admin.getRole() != User.Role.SUPER_ADMIN)) {
            response.put("success", false);
            response.put("message", "Недостаточно прав");
            return response;
        }

        try {
            complaintService.rejectComplaint(id, admin.getId(), admin.getLogin());
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/api/complaints/{id}/resolve")
    @ResponseBody
    public Map<String, Object> resolveComplaint(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return response;
        }

        User admin = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (admin == null || (admin.getRole() != User.Role.ADMIN && admin.getRole() != User.Role.SUPER_ADMIN)) {
            response.put("success", false);
            response.put("message", "Недостаточно прав");
            return response;
        }

        try {
            complaintService.resolveComplaint(id, admin.getId(), admin.getLogin());
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/api/complaints/cleanup")
    @ResponseBody
    public Map<String, Object> cleanupOldComplaints() {
        Map<String, Object> response = new HashMap<>();
        try {
            int deletedCount = complaintService.deleteOldResolvedComplaints(90);
            response.put("success", true);
            response.put("deletedCount", deletedCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}