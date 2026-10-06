package com.directoriocristiano.controller;

import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.dto.ModerationDecisionRequest;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.service.IModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Moderación (contracts/moderation.md). SecurityConfig exige ROLE_MODERATOR en todo /moderation/**. */
@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final IModerationService moderationService;

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
