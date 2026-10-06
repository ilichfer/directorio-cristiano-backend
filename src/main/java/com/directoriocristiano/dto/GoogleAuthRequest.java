package com.directoriocristiano.dto;

import com.directoriocristiano.model.enums.UserType;

public record GoogleAuthRequest(
        String idToken,
        String demoEmail,
        String demoDisplayName,
        UserType userType,
        Boolean acceptAgreement
) {}
