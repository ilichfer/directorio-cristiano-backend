package com.directoriocristiano.controller;

import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.service.IProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final IProfileService profileService;

    @PostMapping("/entrepreneur-profile")
    public ResponseEntity<UserProfileResponse> activateEntrepreneur(
            @RequestBody(required = false) ActivateEntrepreneurRequest request,
            @AuthenticationPrincipal User user) {
        boolean accepted = request != null && Boolean.TRUE.equals(request.acceptAgreement());
        return ResponseEntity.ok(profileService.activateEntrepreneur(user, accepted));
    }

    @DeleteMapping("/entrepreneur-profile")
    public ResponseEntity<UserProfileResponse> deactivateEntrepreneur(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.deactivateEntrepreneur(user));
    }

    public record ActivateEntrepreneurRequest(Boolean acceptAgreement) {}
}
