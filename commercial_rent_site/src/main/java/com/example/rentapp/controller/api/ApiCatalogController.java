//// src/main/java/com/example/rentapp/controller/api/ApiCatalogController.java
//package com.example.rentapp.controller.api;
//
//import com.example.rentapp.entity.Premise;
//import com.example.rentapp.repository.PremiseRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/catalog")
//public class ApiCatalogController {
//
//    @Autowired
//    private PremiseRepository premiseRepository;
//
//    @GetMapping
//    public ResponseEntity<List<Premise>> getAllPremises() {
//        return ResponseEntity.ok(premiseRepository.findAllByActiveTrueOrderByCreatedAtDesc());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Premise> getPremise(@PathVariable Long id) {
//        return premiseRepository.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//}