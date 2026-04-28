package com.example.rentapp.client;

import com.example.rentapp.dto.CommentDto;
import com.example.rentapp.dto.PremiseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // НОВЫЙ МЕТОД: получение помещений по ID владельца
    @GetMapping("/api/premises/owner/{ownerId}")
    List<PremiseDto> getPremisesByOwnerId(@PathVariable("ownerId") Long ownerId);

    @PostMapping("/api/comments")
    CommentDto addComment(@RequestBody CommentDto commentDto);

    @GetMapping("/api/comments/premise/{premiseId}")
    List<CommentDto> getCommentsByPremiseId(@PathVariable("premiseId") Long premiseId);
}