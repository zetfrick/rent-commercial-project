package com.example.rentapp.controller;

import com.example.rentapp.client.CatalogClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/premise")
public class PremiseApiController {

    @Autowired
    private CatalogClient catalogClient;

    @PostMapping("/{id}/toggle-publish")
    public ResponseEntity<Map<String, Object>> togglePublish(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Map<String, Object> result = catalogClient.togglePublish(id, request);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deletePremise(@PathVariable Long id) {
        Map<String, Object> result = catalogClient.deletePremise(id);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}