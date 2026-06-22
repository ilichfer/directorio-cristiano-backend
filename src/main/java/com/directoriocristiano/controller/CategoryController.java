package com.directoriocristiano.controller;

import com.directoriocristiano.model.entity.Category;
import com.directoriocristiano.model.entity.Zone;
import com.directoriocristiano.repository.CategoryRepository;
import com.directoriocristiano.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;

    @GetMapping("/api/v1/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> names = categoryRepository.findAll().stream()
                .map(Category::getName)
                .toList();
        return ResponseEntity.ok(names);
    }

    @GetMapping("/api/v1/zones")
    public ResponseEntity<List<String>> getZones() {
        List<String> names = zoneRepository.findAll().stream()
                .map(Zone::getName)
                .toList();
        return ResponseEntity.ok(names);
    }
}
