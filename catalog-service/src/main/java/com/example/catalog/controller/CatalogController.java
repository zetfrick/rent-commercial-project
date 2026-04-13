package com.example.catalog.controller;

import com.example.catalog.dto.PremiseDto;        // ← ВАЖНО: свой DTO из catalog
import com.example.catalog.entity.Premise;
import com.example.catalog.repository.PremiseRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    @Autowired
    private PremiseRepository premiseRepository;

    @GetMapping("/catalog")
    public ResponseEntity<List<Premise>> getAll() {
        return ResponseEntity.ok(premiseRepository.findAllByActiveTrueOrderByCreatedAtDesc());
    }

    @GetMapping("/catalog/{id}")
    public ResponseEntity<Premise> getById(@PathVariable Long id) {
        return premiseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/premise/add")
    public ResponseEntity<Premise> addPremise(@RequestBody PremiseDto premiseDto) {
        Premise premise = new Premise();
        BeanUtils.copyProperties(premiseDto, premise, "id", "createdAt");

        premise.setLatitude(premiseDto.getLatitude());
        premise.setLongitude(premiseDto.getLongitude());

        Premise saved = premiseRepository.save(premise);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/premises/latest")
    public ResponseEntity<List<Premise>> getLatestPremises(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {

        List<Premise> latest = premiseRepository.findTop6ByActiveTrueOrderByCreatedAtDesc();
        return ResponseEntity.ok(latest);
    }
}