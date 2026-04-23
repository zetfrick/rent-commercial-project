package com.example.rentapp.client;

import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/catalog")
    List<PremiseDto> getAllPremises();

    @GetMapping("/api/catalog/{id}")
    PremiseDto getPremiseById(@PathVariable("id") Long id);

    @PostMapping("/api/premise/add")
    PremiseDto addPremise(@RequestBody PremiseDto premiseDto);

    /**
     * Новый метод: получение последних добавленных помещений
     */
    @GetMapping("/api/premises/latest")
    List<PremiseDto> getLatestPremises(@RequestParam(value = "limit", defaultValue = "6") int limit);

    // ==================== КОММЕНТАРИИ ====================

    @PostMapping("/api/comments")
    CommentDto addComment(@RequestBody CommentDto commentDto);

    @GetMapping("/api/comments/premise/{premiseId}")
    List<CommentDto> getCommentsByPremiseId(@PathVariable("premiseId") Long premiseId);
}