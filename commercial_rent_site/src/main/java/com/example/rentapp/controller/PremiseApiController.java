package com.example.rentapp.controller;

import com.example.rentapp.service.CatalogServiceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/premise")
public class PremiseApiController {

    @Autowired
    private CatalogServiceWrapper catalogService;

    @PostMapping("/{id}/toggle-publish")
    public ResponseEntity<Map<String, Object>> togglePublish(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Map<String, Object> result = catalogService.togglePublish(id, request);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deletePremise(@PathVariable Long id) {
        Map<String, Object> result = catalogService.deletePremise(id);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}