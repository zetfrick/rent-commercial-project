package com.example.rentapp.controller;

import com.example.rentapp.dto.ComplaintDto;
import com.example.rentapp.entity.User;
import com.example.rentapp.service.ComplaintService;
import com.example.rentapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintApiController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserService userService;

    @PostMapping
    public Map<String, Object> createComplaint(@RequestBody ComplaintDto complaintDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Необходимо авторизоваться");
            return response;
        }

        User complainant = userService.findByLogin(userDetails.getUsername()).orElse(null);
        if (complainant == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return response;
        }

        complaintDto.setComplainantId(complainant.getId());
        complaintDto.setComplainantName(complainant.getLogin());

        try {
            complaintService.saveComplaint(complaintDto);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}