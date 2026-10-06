package com.directoriocristiano.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Ficha de negocio enviada por su dueño. Solo el nombre es obligatorio al guardar: un borrador
 * puede estar incompleto y la completitud se valida al enviarlo a revisión (FR-008).
 */
public record BusinessRequest(
        @NotBlank String name,
        String category,
        String zone,
        String description,
        String slogan,
        String logoUrl,
        String coverUrl,
        List<String> values,

        String contactPhone,
        String contactWhatsapp,
        String contactEmail,
        String contactWebsite,
        String contactAddress,

        @Valid List<ServiceRequest> services
) {
    /** Con {@code id}: servicio existente. Sin {@code id}: servicio nuevo (research R4). */
    public record ServiceRequest(
            UUID id,
            @NotBlank String name,
            String price,
            String description
    ) {}
}
