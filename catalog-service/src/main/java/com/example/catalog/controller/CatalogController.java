package com.example.catalog.controller;

import com.example.catalog.dto.CommentDto;
import com.example.catalog.dto.PremiseDto;
import com.example.catalog.entity.Comment;
import com.example.catalog.entity.Premise;
import com.example.catalog.repository.CommentRepository;
import com.example.catalog.repository.PremiseRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    @Autowired
    private PremiseRepository premiseRepository;

    @Autowired
    private CommentRepository commentRepository;

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
    public ResponseEntity<List<Premise>> getLatestPremises(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        List<Premise> latest = premiseRepository.findTop6ByActiveTrueOrderByCreatedAtDesc();
        return ResponseEntity.ok(latest);
    }

    // НОВЫЙ МЕТОД: получение помещений по ID владельца
    @GetMapping("/premises/owner/{ownerId}")
    public ResponseEntity<List<PremiseDto>> getPremisesByOwnerId(@PathVariable Long ownerId) {
        List<Premise> premises = premiseRepository.findByOwnerId(ownerId);

        List<PremiseDto> dtos = premises.stream().map(p -> {
            PremiseDto dto = new PremiseDto();
            BeanUtils.copyProperties(p, dto);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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