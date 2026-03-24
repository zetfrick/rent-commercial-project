//// src/main/java/com/example/rentapp/controller/api/ApiPremiseController.java
//package com.example.rentapp.controller.api;
//
//import com.example.rentapp.dto.PremiseForm;
//import com.example.rentapp.entity.Premise;
//import com.example.rentapp.entity.User;
//import com.example.rentapp.repository.PremiseRepository;
//import com.example.rentapp.service.FileStorageService;
//import com.example.rentapp.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/premise")
//public class ApiPremiseController {
//
//    @Autowired private PremiseRepository premiseRepository;
//    @Autowired private UserService userService;
//    @Autowired private FileStorageService fileStorageService;
//
//    @PostMapping("/add")
//    public ResponseEntity<Premise> addPremise(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestPart("form") PremiseForm form,
//            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
//
//        User owner = userService.findByLogin(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        Premise premise = new Premise();
//        premise.setOwner(owner);
//        // ... (заполни все поля как в ProfileController — можно вынести в сервис)
//        // Для краткости — используй код из твоего ProfileController
//
//        if (photos != null && !photos.isEmpty()) {
//            List<String> paths = fileStorageService.savePhotos(photos);
//            premise.getPhotoPaths().addAll(paths);
//        }
//
//        Premise saved = premiseRepository.save(premise);
//        return ResponseEntity.ok(saved);
//    }
//}