package com.example.rentapp.client;

import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/catalog")
    List<PremiseDto> getAllPremises();

    @GetMapping("/api/catalog/{id}")
    PremiseDto getPremiseById(@PathVariable("id") Long id);

    @PostMapping("/api/premise/add")
    PremiseDto addPremise(@RequestBody PremiseDto premiseDto);

    @PutMapping("/api/premise/{id}")
    PremiseDto updatePremise(@PathVariable("id") Long id, @RequestBody PremiseDto premiseDto);

    @GetMapping("/api/premises/latest")
    List<PremiseDto> getLatestPremises(@RequestParam(value = "limit", defaultValue = "6") int limit);

    @GetMapping("/api/premises/owner/{ownerId}")
    List<PremiseDto> getPremisesByOwnerId(@PathVariable("ownerId") Long ownerId);

    @PostMapping("/api/comments")
    CommentDto addComment(@RequestBody CommentDto commentDto);

    @GetMapping("/api/comments/premise/{premiseId}")
    List<CommentDto> getCommentsByPremiseId(@PathVariable("premiseId") Long premiseId);

    // НОВЫЙ МЕТОД: получение конфигурации
    @GetMapping("/api/config/all")
    Map<String, Object> getAllConfig();

    @PostMapping("/api/premise/{id}/toggle-publish")
    Map<String, Object> togglePublish(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> request);

    @DeleteMapping("/api/premise/{id}/delete")
    Map<String, Object> deletePremise(@PathVariable("id") Long id);
}