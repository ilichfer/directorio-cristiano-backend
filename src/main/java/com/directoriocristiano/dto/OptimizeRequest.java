package com.directoriocristiano.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record OptimizeRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String rawDescription,
        List<String> coreValues,
        String bibleAspiration
) {}
