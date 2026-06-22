package com.directoriocristiano.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BusinessRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String zone,
        @NotBlank String description,
        String slogan,
        String logoUrl,
        String coverUrl,
        List<String> values,

        @NotBlank String contactPhone,
        @NotBlank String contactWhatsapp,
        @NotBlank String contactEmail,
        String contactWebsite,
        @NotBlank String contactAddress,

        @Valid List<ServiceRequest> services
) {
    public record ServiceRequest(
            @NotBlank String name,
            String price,
            String description
    ) {}
}
