package com.directoriocristiano.dto;

import java.util.List;

public record OptimizeResponse(
        String optimizedDescription,
        String recommendedSlogan,
        List<String> generatedTags,
        String biblicalConnectionQuote
) {}
