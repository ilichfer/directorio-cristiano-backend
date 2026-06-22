package com.directoriocristiano.controller;

import com.directoriocristiano.dto.*;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.service.IBusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final IBusinessService businessService;

    @GetMapping
    public ResponseEntity<PageResponse<BusinessResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String zone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(
                businessService.getAll(search, category, zone, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(businessService.getById(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BusinessResponse>> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(businessService.getMyBusinesses(user));
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @Valid @RequestBody BusinessRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.create(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(businessService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        businessService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(businessService.addReview(id, request, user));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(businessService.getReviews(id, page, size));
    }
}
