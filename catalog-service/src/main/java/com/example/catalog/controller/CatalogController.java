package com.example.catalog.controller;

import com.example.catalog.dto.CommentDto;
import com.example.catalog.dto.PremiseDto;
import com.example.catalog.entity.Comment;
import com.example.catalog.entity.Premise;
import com.example.catalog.repository.CommentRepository;
import com.example.catalog.repository.PremiseRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@EnableScheduling
public class CatalogController {

    @Autowired
    private PremiseRepository premiseRepository;

    @Autowired
    private CommentRepository commentRepository;

    // Выполняется сразу после запуска приложения
    @PostConstruct
    public void init() {
        System.out.println("=== Выполнение первоначальной проверки просроченных объявлений ===");
        checkAndUnpublishExpiredPremises();
        System.out.println("=== Выполнение первоначальной проверки удаления старых объявлений ===");
        deleteOldUnpublishedPremises();
        System.out.println("=== Первоначальная проверка завершена ===");
    }

    // Запускается каждый день в 00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void checkAndUnpublishExpiredPremises() {
        LocalDate today = LocalDate.now();
        // Находим активные объявления, у которых дата окончания доступности уже прошла
        List<Premise> expiredPremises = premiseRepository.findByActiveTrueAndAvailableToBefore(today);

        System.out.println("Найдено просроченных объявлений: " + expiredPremises.size());
        for (Premise premise : expiredPremises) {
            premise.setActive(false);
            premise.setUnpublishedAt(LocalDateTime.now());
            premiseRepository.save(premise);
            System.out.println("Объявление #" + premise.getId() + " снято с публикации (истек срок доступности)");
        }
    }

    // Запускается каждый день в 01:00
    @Scheduled(cron = "0 0 1 * * *")
    public void deleteOldUnpublishedPremises() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(60);
        // Находим неактивные объявления, которые были сняты более 60 дней назад
        List<Premise> oldPremises = premiseRepository.findByActiveFalseAndUnpublishedAtBefore(cutoffDate);

        System.out.println("Найдено объявлений для удаления: " + oldPremises.size());
        for (Premise premise : oldPremises) {
            // Удаляем фото с диска
            List<String> photoPaths = premise.getPhotoPaths();
            if (photoPaths != null && !photoPaths.isEmpty()) {
                String uploadDir = "uploads/";
                for (String photoPath : photoPaths) {
                    try {
                        Path filePath = Paths.get(uploadDir + photoPath);
                        Files.deleteIfExists(filePath);
                        System.out.println("Удалён файл: " + photoPath);
                    } catch (IOException e) {
                        System.err.println("Ошибка удаления файла: " + photoPath + " - " + e.getMessage());
                    }
                }
            }

            // Удаляем комментарии
            commentRepository.deleteByPremiseId(premise.getId());

            // Удаляем объявление
            premiseRepository.delete(premise);
            System.out.println("Объявление #" + premise.getId() + " полностью удалено (снято более 60 дней назад)");
        }
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<PremiseDto>> getAll() {
        List<Premise> premises = premiseRepository.findAllByActiveTrueOrderByCreatedAtDesc();

        List<PremiseDto> dtos = premises.stream().map(p -> {
            PremiseDto dto = new PremiseDto();
            BeanUtils.copyProperties(p, dto);
            dto.setUnpublishedAt(p.getUnpublishedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/catalog/{id}")
    public ResponseEntity<PremiseDto> getById(@PathVariable Long id) {
        return premiseRepository.findById(id)
                .map(p -> {
                    PremiseDto dto = new PremiseDto();
                    BeanUtils.copyProperties(p, dto);
                    dto.setUnpublishedAt(p.getUnpublishedAt());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/premise/add")
    public ResponseEntity<Premise> addPremise(@RequestBody PremiseDto premiseDto) {
        Premise premise = new Premise();
        BeanUtils.copyProperties(premiseDto, premise, "id", "createdAt", "unpublishedAt");
        premise.setLatitude(premiseDto.getLatitude());
        premise.setLongitude(premiseDto.getLongitude());

        Premise saved = premiseRepository.save(premise);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/premise/{id}")
    public ResponseEntity<Premise> updatePremise(@PathVariable Long id, @RequestBody PremiseDto premiseDto) {
        Premise existingPremise = premiseRepository.findById(id).orElse(null);
        if (existingPremise == null) {
            return ResponseEntity.notFound().build();
        }

        existingPremise.setType(premiseDto.getType());
        existingPremise.setArea(premiseDto.getArea());
        existingPremise.setCapacity(premiseDto.getCapacity());
        existingPremise.setAmenities(premiseDto.getAmenities());
        existingPremise.setDescription(premiseDto.getDescription());
        existingPremise.setExtraFees(premiseDto.getExtraFees());
        existingPremise.setImportantInfo(premiseDto.getImportantInfo());

        Premise saved = premiseRepository.save(existingPremise);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/premises/latest")
    public ResponseEntity<List<PremiseDto>> getLatestPremises(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        List<Premise> latest = premiseRepository.findTop6ByActiveTrueOrderByCreatedAtDesc();

        List<PremiseDto> dtos = latest.stream().map(p -> {
            PremiseDto dto = new PremiseDto();
            BeanUtils.copyProperties(p, dto);
            dto.setUnpublishedAt(p.getUnpublishedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/premises/owner/{ownerId}")
    public ResponseEntity<List<PremiseDto>> getPremisesByOwnerId(@PathVariable Long ownerId) {
        List<Premise> premises = premiseRepository.findByOwnerId(ownerId);

        List<PremiseDto> dtos = premises.stream().map(p -> {
            PremiseDto dto = new PremiseDto();
            BeanUtils.copyProperties(p, dto);
            dto.setUnpublishedAt(p.getUnpublishedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/premise/{id}/toggle-publish")
    public ResponseEntity<Map<String, Object>> togglePublish(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Map<String, Object> response = new HashMap<>();
        Optional<Premise> premiseOpt = premiseRepository.findById(id);

        if (premiseOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Помещение не найдено");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Premise premise = premiseOpt.get();
        Boolean active = request.get("active");

        if (active != null) {
            premise.setActive(active);
            if (!active && premise.getUnpublishedAt() == null) {
                premise.setUnpublishedAt(LocalDateTime.now());
            }
            premiseRepository.save(premise);
            response.put("success", true);
            response.put("active", active);
        } else {
            response.put("success", false);
            response.put("message", "Не указан статус");
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/premise/{id}/delete")
    public ResponseEntity<Map<String, Object>> deletePremise(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<Premise> premiseOpt = premiseRepository.findById(id);

        if (premiseOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Помещение не найдено");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Premise premise = premiseOpt.get();

        List<String> photoPaths = premise.getPhotoPaths();
        if (photoPaths != null && !photoPaths.isEmpty()) {
            String uploadDir = "uploads/";
            for (String photoPath : photoPaths) {
                try {
                    Path filePath = Paths.get(uploadDir + photoPath);
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Ошибка удаления файла: " + photoPath);
                }
            }
        }

        commentRepository.deleteByPremiseId(id);
        premiseRepository.deleteById(id);

        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/premise/{premiseId}")
    public ResponseEntity<List<CommentDto>> getCommentsByPremiseId(@PathVariable Long premiseId) {
        List<Comment> comments = commentRepository.findByPremiseIdOrderByCreatedAtDesc(premiseId);

        List<CommentDto> dtos = comments.stream().map(c ->
                new CommentDto(
                        c.getId(),
                        c.getPremiseId(),
                        c.getAuthorName(),
                        c.getText(),
                        c.getCreatedAt()
                )
        ).toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/comments")
    public ResponseEntity<CommentDto> addComment(@RequestBody CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Comment comment = new Comment(
                commentDto.getPremiseId(),
                commentDto.getAuthorName(),
                commentDto.getText()
        );

        Comment saved = commentRepository.save(comment);

        CommentDto response = new CommentDto(
                saved.getId(),
                saved.getPremiseId(),
                saved.getAuthorName(),
                saved.getText(),
                saved.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}