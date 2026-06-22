package com.directoriocristiano.controller;

import com.directoriocristiano.model.entity.Category;
import com.directoriocristiano.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final CategoryRepository categoryRepository;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "pong",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/insert")
    public ResponseEntity<Map<String, Object>> insert() {
        var testCategory = Category.builder()
            .name("Test-" + System.currentTimeMillis())
            .displayOrder(999)
            .build();
        var saved = categoryRepository.save(testCategory);
        return ResponseEntity.ok(Map.of(
            "status", "inserted",
            "id", saved.getId().toString(),
            "name", saved.getName()
        ));
    }
}
