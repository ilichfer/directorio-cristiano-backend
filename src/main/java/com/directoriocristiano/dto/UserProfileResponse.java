package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.AuthProvider;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.model.enums.VerificationStep;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        UserType userType,
        boolean isEntrepreneur,
        boolean isModerator,
        Instant entrepreneurAgreementAt,
        boolean isVerified,
        boolean pastoralVerification,
        String church,
        String pastorName,
        VerificationStep verificationStep,
        AuthProvider authProvider
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getUserType(),
                user.getUserType() == UserType.entrepreneur,
                user.isModerator(),
                user.getEntrepreneurAgreementAt(),
                user.isVerified(),
                user.isPastoralVerification(),
                user.getChurch(),
                user.getPastorName(),
                user.getVerificationStep(),
                user.getAuthProvider()
        );
    }
}
