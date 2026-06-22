package com.directoriocristiano.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String displayName,
        String token,
        String refreshToken
) {}
