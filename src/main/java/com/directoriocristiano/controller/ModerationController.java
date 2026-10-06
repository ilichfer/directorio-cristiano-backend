package com.directoriocristiano.controller;

import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.dto.ModerationDecisionRequest;
import com.directoriocristiano.dto.ModerationEventResponse;
import com.directoriocristiano.dto.ModeratedBusinessItem;
import com.directoriocristiano.dto.ModerationInboxItem;
import com.directoriocristiano.dto.PageResponse;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.service.IModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Moderación (contracts/moderation.md). SecurityConfig exige ROLE_MODERATOR en todo /moderation/**. */
@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final IModerationService moderationService;

    @GetMapping("/requests")
    public ResponseEntity<PageResponse<ModerationInboxItem>> inbox(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moderationService.inbox(status, page, size, user));
    }

    @GetMapping("/businesses")
    public ResponseEntity<PageResponse<ModeratedBusinessItem>> businesses(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moderationService.listByStatus(status, page, size, user));
    }

    @PostMapping("/businesses/{id}/suspend")
    public ResponseEntity<Void> suspend(
            @PathVariable UUID id,
            @RequestBody ModerationDecisionRequest request,
            @AuthenticationPrincipal User user) {
        moderationService.suspend(id, request.reason(), user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/businesses/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        moderationService.reactivate(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/businesses/{id}/history")
    public ResponseEntity<List<ModerationEventResponse>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(moderationService.history(id));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<ChangeRequestDetail> detail(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moderationService.detail(id, user));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<ChangeRequestDetail> approve(
            @PathVariable UUID id,
            @RequestBody ModerationDecisionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moderationService.approve(id, request.expectedVersion(), user));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ChangeRequestDetail> reject(
            @PathVariable UUID id,
            @RequestBody ModerationDecisionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moderationService.reject(id, request.expectedVersion(), request.reason(), user));
    }
}
