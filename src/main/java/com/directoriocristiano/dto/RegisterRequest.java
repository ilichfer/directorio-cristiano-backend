package com.directoriocristiano.dto;

import com.directoriocristiano.model.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String displayName,
        @NotNull UserType userType,
        String church,
        String pastorName
) {}
