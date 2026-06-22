package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.model.enums.VerificationStep;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        UserType userType,
        boolean isVerified,
        boolean pastoralVerification,
        String church,
        String pastorName,
        VerificationStep verificationStep
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getUserType(),
                user.isVerified(),
                user.isPastoralVerification(),
                user.getChurch(),
                user.getPastorName(),
                user.getVerificationStep()
        );
    }
}
