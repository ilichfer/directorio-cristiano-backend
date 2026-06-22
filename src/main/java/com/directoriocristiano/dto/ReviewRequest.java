package com.directoriocristiano.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotBlank String authorName,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String comment,
        @NotNull @Min(1) @Max(5) Integer honesty,
        @NotNull @Min(1) @Max(5) Integer quality,
        @NotNull @Min(1) @Max(5) Integer punctuality,
        @NotNull @Min(1) @Max(5) Integer kindness
) {}
