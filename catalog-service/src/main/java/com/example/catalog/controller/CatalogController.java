package com.example.catalog.controller;

import com.example.catalog.config.PremiseConfig;
import com.example.catalog.dto.CommentDto;
import com.example.catalog.dto.PremiseDto;
import com.example.catalog.entity.Comment;
import com.example.catalog.entity.Premise;
import com.example.catalog.repository.CommentRepository;
import com.example.catalog.repository.PremiseRepository;
import com.example.catalog.service.NotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${main.service.url:http://localhost:8080}")
    private String mainServiceUrl;

    // Выполняется после полного запуска приложения (вместо @PostConstruct)
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        System.out.println("=== ApplicationReadyEvent: Начало проверки ===");
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
        System.out.println("=== [SCHEDULED] Проверка просроченных объявлений на " + today + " ===");

        // Находим активные объявления, у которых дата окончания доступности уже прошла
        List<Premise> expiredPremises = premiseRepository.findByActiveTrueAndAvailableToBefore(today);

        System.out.println("Найдено просроченных объявлений: " + expiredPremises.size());

        for (Premise premise : expiredPremises) {
            System.out.println("---");
            System.out.println("Обработка объявления #" + premise.getId());
            System.out.println("Тип: " + premise.getType());
            System.out.println("Владелец ID: " + premise.getOwnerId());
            System.out.println("Дата доступности до: " + premise.getAvailableTo());

            // Проверяем наличие владельца
            if (premise.getOwnerId() == null) {
                System.err.println("!!! ПРЕДУПРЕЖДЕНИЕ: У объявления #" + premise.getId() + " отсутствует владелец (owner_id = null)");
                System.err.println("!!! Уведомление не будет отправлено");
                premise.setActive(false);
                premise.setUnpublishedAt(LocalDateTime.now());
                premiseRepository.save(premise);
                System.out.println("Объявление #" + premise.getId() + " снято с публикации (без уведомления владельца)");
                continue;
            }

            // Проверяем, не заблокирован ли пользователь
            if (isUserBanned(premise.getOwnerId())) {
                System.out.println("⚠ Владелец #" + premise.getOwnerId() + " заблокирован, объявление снимается без уведомления");
                premise.setActive(false);
                premise.setUnpublishedAt(LocalDateTime.now());
                premiseRepository.save(premise);
                System.out.println("Объявление #" + premise.getId() + " снято с публикации (владелец заблокирован)");
                continue;
            }

            premise.setActive(false);
            premise.setUnpublishedAt(LocalDateTime.now());
            premiseRepository.save(premise);
            System.out.println("✓ Объявление #" + premise.getId() + " снято с публикации (истек срок доступности)");

            // Отправляем уведомление владельцу о снятии помещения с русским названием типа
            try {
                System.out.println("→ Попытка отправить уведомление владельцу ID=" + premise.getOwnerId());
                String typeInRussian = PremiseConfig.TYPE_RUSSIAN.getOrDefault(premise.getType(), premise.getType());
                notificationService.sendNotification(
                        premise.getOwnerId(),
                        "PREMISE_EXPIRED",
                        premise.getId(),
                        null,
                        null,
                        "Помещение \"" + typeInRussian + "\" снято с публикации (истек срок доступности)",
                        "/premise/" + premise.getId()
                );
                System.out.println("✓ Уведомление отправлено владельцу");
            } catch (Exception e) {
                System.err.println("✗ ОШИБКА отправки уведомления для помещения #" + premise.getId());
                System.err.println("  Сообщение: " + e.getMessage());
            }
            System.out.println("---");
        }
        System.out.println("=== [SCHEDULED] Проверка просроченных объявлений завершена ===");
    }

    // Запускается каждый день в 01:00
    @Scheduled(cron = "0 0 1 * * *")
    public void deleteOldUnpublishedPremises() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(60);
        System.out.println("=== [SCHEDULED] Проверка старых объявлений для удаления (до " + cutoffDate + ") ===");

        List<Premise> oldPremises = premiseRepository.findByActiveFalseAndUnpublishedAtBefore(cutoffDate);

        System.out.println("Найдено объявлений для удаления: " + oldPremises.size());

        for (Premise premise : oldPremises) {
            System.out.println("---");
            System.out.println("Удаление объявления #" + premise.getId());
            if (premise.getUnpublishedAt() != null) {
                System.out.println("  Снято с публикации: " + premise.getUnpublishedAt());
                System.out.println("  Прошло дней: " + java.time.Duration.between(premise.getUnpublishedAt(), LocalDateTime.now()).toDays());
            }

            // Удаляем фото с диска
            List<String> photoPaths = premise.getPhotoPaths();
            if (photoPaths != null && !photoPaths.isEmpty()) {
                String uploadDir = "uploads/";
                int deletedCount = 0;
                for (String photoPath : photoPaths) {
                    try {
                        Path filePath = Paths.get(uploadDir + photoPath);
                        if (Files.deleteIfExists(filePath)) {
                            deletedCount++;
                            System.out.println("  ✓ Удалён файл: " + photoPath);
                        } else {
                            System.out.println("  ⚠ Файл не найден: " + photoPath);
                        }
                    } catch (IOException e) {
                        System.err.println("  ✗ Ошибка удаления файла: " + photoPath + " - " + e.getMessage());
                    }
                }
                System.out.println("  Удалено файлов: " + deletedCount + " из " + photoPaths.size());
            }

            commentRepository.deleteByPremiseId(premise.getId());
            System.out.println("  ✓ Комментарии удалены");

            premiseRepository.delete(premise);
            System.out.println("✓ Объявление #" + premise.getId() + " полностью удалено (снято более 60 дней назад)");
            System.out.println("---");
        }
        System.out.println("=== [SCHEDULED] Удаление старых объявлений завершено ===");
    }

    @PostMapping("/internal/users/{userId}/unpublish-all")
    public ResponseEntity<Map<String, Object>> unpublishAllUserPremises(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("=== Снятие всех объявлений пользователя #" + userId + " ===");

        List<Premise> userPremises = premiseRepository.findByOwnerId(userId);
        int unpublishedCount = 0;

        for (Premise premise : userPremises) {
            if (premise.isActive()) {
                premise.setActive(false);
                premise.setUnpublishedAt(LocalDateTime.now());
                premiseRepository.save(premise);
                unpublishedCount++;
                System.out.println("✓ Объявление #" + premise.getId() + " снято с публикации");

                // ===== ДОБАВЬТЕ ОТПРАВКУ УВЕДОМЛЕНИЯ ДЛЯ КАЖДОГО ОБЪЯВЛЕНИЯ =====
                try {
                    String typeInRussian = PremiseConfig.TYPE_RUSSIAN.getOrDefault(premise.getType(), premise.getType());
                    notificationService.sendNotification(
                            userId,
                            "PREMISE_EXPIRED",
                            premise.getId(),
                            null,
                            null,
                            "Помещение \"" + typeInRussian + "\" снято с публикации (аккаунт заблокирован)",
                            "/premise/" + premise.getId()
                    );
                    System.out.println("  ✓ Уведомление отправлено владельцу #" + userId);
                } catch (Exception e) {
                    System.err.println("  ✗ Ошибка отправки уведомления для объявления #" + premise.getId() + ": " + e.getMessage());
                }
                // ===== КОНЕЦ БЛОКА =====
            }
        }

        response.put("success", true);
        response.put("unpublishedCount", unpublishedCount);
        System.out.println("=== Снято объявлений: " + unpublishedCount + " ===");

        return ResponseEntity.ok(response);
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

        System.out.println("=== Добавление нового помещения ===");
        System.out.println("OwnerId: " + premiseDto.getOwnerId());
        System.out.println("Тип: " + premiseDto.getType());

        // Проверяем, не заблокирован ли пользователь
        if (premiseDto.getOwnerId() != null && isUserBanned(premiseDto.getOwnerId())) {
            System.err.println("!!! Ошибка: Пользователь #" + premiseDto.getOwnerId() + " заблокирован и не может добавлять помещения");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Premise premise = new Premise();
        BeanUtils.copyProperties(premiseDto, premise, "id", "createdAt", "unpublishedAt");
        premise.setLatitude(premiseDto.getLatitude());
        premise.setLongitude(premiseDto.getLongitude());

        if (premise.getOwnerId() == null) {
            System.err.println("!!! ВНИМАНИЕ: Помещение добавляется без владельца (owner_id = null)");
        }

        Premise saved = premiseRepository.save(premise);
        System.out.println("✓ Помещение #" + saved.getId() + " успешно добавлено");
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/premise/{id}")
    public ResponseEntity<Premise> updatePremise(@PathVariable Long id, @RequestBody PremiseDto premiseDto) {
        Premise existingPremise = premiseRepository.findById(id).orElse(null);
        if (existingPremise == null) {
            return ResponseEntity.notFound().build();
        }

        // Проверяем, не заблокирован ли владелец
        if (existingPremise.getOwnerId() != null && isUserBanned(existingPremise.getOwnerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
            boolean wasActive = premise.isActive();

            System.out.println("=== Ручное изменение статуса публикации ===");
            System.out.println("Помещение #" + id);
            System.out.println("Было активно: " + wasActive);
            System.out.println("Стало активно: " + active);
            System.out.println("Владелец ID: " + premise.getOwnerId());

            // Проверяем, не заблокирован ли владелец (нельзя опубликовать если заблокирован)
            if (active && premise.getOwnerId() != null && isUserBanned(premise.getOwnerId())) {
                response.put("success", false);
                response.put("message", "Невозможно опубликовать объявление - ваш аккаунт заблокирован");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            premise.setActive(active);
            if (!active && premise.getUnpublishedAt() == null) {
                premise.setUnpublishedAt(LocalDateTime.now());
                System.out.println("Установлена дата снятия: " + premise.getUnpublishedAt());
            }
            premiseRepository.save(premise);
            response.put("success", true);
            response.put("active", active);

            if (wasActive && !active) {
                if (premise.getOwnerId() == null) {
                    System.err.println("!!! ПРЕДУПРЕЖДЕНИЕ: У помещения #" + id + " отсутствует владелец, уведомление не отправлено");
                    response.put("notificationSent", false);
                    response.put("notificationError", "Отсутствует владелец помещения");
                } else if (!isUserBanned(premise.getOwnerId())) {
                    try {
                        System.out.println("→ Попытка отправить уведомление владельцу ID=" + premise.getOwnerId());
                        String typeInRussian = PremiseConfig.TYPE_RUSSIAN.getOrDefault(premise.getType(), premise.getType());
                        notificationService.sendNotification(
                                premise.getOwnerId(),
                                "PREMISE_EXPIRED",
                                premise.getId(),
                                null,
                                null,
                                "Помещение \"" + typeInRussian + "\" снято с публикации",
                                "/premise/" + premise.getId()
                        );
                        System.out.println("✓ Уведомление успешно отправлено владельцу о снятии помещения #" + premise.getId());
                        response.put("notificationSent", true);
                    } catch (Exception e) {
                        System.err.println("✗ ОШИБКА отправки уведомления для помещения #" + premise.getId());
                        System.err.println("  Сообщение: " + e.getMessage());
                        response.put("notificationSent", false);
                        response.put("notificationError", e.getMessage());
                    }
                } else {
                    System.out.println("Уведомление не отправлено (владелец заблокирован)");
                }
            } else {
                System.out.println("Уведомление не требуется (помещение не было снято с публикации)");
            }
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
        System.out.println("=== Удаление помещения #" + id + " ===");

        List<String> photoPaths = premise.getPhotoPaths();
        if (photoPaths != null && !photoPaths.isEmpty()) {
            String uploadDir = "uploads/";
            for (String photoPath : photoPaths) {
                try {
                    Path filePath = Paths.get(uploadDir + photoPath);
                    Files.deleteIfExists(filePath);
                    System.out.println("✓ Удалён файл: " + photoPath);
                } catch (IOException e) {
                    System.err.println("✗ Ошибка удаления файла: " + photoPath);
                }
            }
        }

        commentRepository.deleteByPremiseId(id);
        premiseRepository.deleteById(id);

        response.put("success", true);
        System.out.println("✓ Помещение #" + id + " удалено");
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
                        null,
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

        // Проверяем, не заблокирован ли пользователь по authorId
        if (commentDto.getAuthorId() != null && isUserBanned(commentDto.getAuthorId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
                commentDto.getAuthorId(),
                saved.getText(),
                saved.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Комментарий не найден");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        commentRepository.deleteById(id);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/services")
    public Map<String, Object> debugServices() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("status", "ok");
            result.put("catalog-service.port", "8081");
            result.put("main-service.url", "http://localhost:8080");
            result.put("notification.service.available", "true");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ==================== МЕТОДЫ ДЛЯ ПРОВЕРКИ БЛОКИРОВКИ ПОЛЬЗОВАТЕЛЯ ====================

    /**
     * Проверяет, заблокирован ли пользователь
     * @param userId ID пользователя
     * @return true если пользователь заблокирован
     */
    private boolean isUserBanned(Long userId) {
        if (userId == null) return false;

        try {
            String url = mainServiceUrl + "/api/internal/users/" + userId + "/banned";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("banned")) {
                return (boolean) response.getBody().get("banned");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке блокировки пользователя " + userId + ": " + e.getMessage());
        }
        return false;
    }

    @PutMapping("/premises/owner/{ownerId}/update-contacts")
    public ResponseEntity<Void> updateOwnerContacts(@PathVariable Long ownerId,
                                                    @RequestBody Map<String, String> contacts) {
        System.out.println("=== Обновление контактных данных владельца #" + ownerId + " ===");

        List<Premise> ownerPremises = premiseRepository.findByOwnerId(ownerId);

        if (ownerPremises.isEmpty()) {
            System.out.println("У владельца #" + ownerId + " нет объявлений");
            return ResponseEntity.ok().build();
        }

        String firstName = contacts.get("firstName");
        String lastName = contacts.get("lastName");
        String middleName = contacts.get("middleName");
        String phone = contacts.get("phone");
        String email = contacts.get("email");

        int updatedCount = 0;

        for (Premise premise : ownerPremises) {
            boolean updated = false;

            if (firstName != null && !firstName.equals(premise.getContactFirstName())) {
                premise.setContactFirstName(firstName);
                updated = true;
            }
            if (lastName != null && !lastName.equals(premise.getContactLastName())) {
                premise.setContactLastName(lastName);
                updated = true;
            }
            if (middleName != null && !middleName.equals(premise.getContactMiddleName())) {
                premise.setContactMiddleName(middleName);
                updated = true;
            }
            if (phone != null && !phone.equals(premise.getContactPhone())) {
                premise.setContactPhone(phone);
                updated = true;
            }
            if (email != null && !email.equals(premise.getContactEmail())) {
                premise.setContactEmail(email);
                updated = true;
            }

            if (updated) {
                premiseRepository.save(premise);
                updatedCount++;
                System.out.println("✓ Обновлено объявление #" + premise.getId());
            }
        }

        System.out.println("Обновлено объявлений: " + updatedCount + " из " + ownerPremises.size());
        System.out.println("=== Обновление контактов завершено ===");

        return ResponseEntity.ok().build();
    }

}