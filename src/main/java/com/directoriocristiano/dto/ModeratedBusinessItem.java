package com.directoriocristiano.dto;

import com.directoriocristiano.model.enums.BusinessStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Negocio listado por estado para el moderador (pestaña "Suspendidos"). {@code ownBusiness}: el
 * moderador es el dueño y no puede reactivarlo (FR-019).
 */
public record ModeratedBusinessItem(
        UUID businessId,
        String businessName,
        String category,
        String ownerName,
        BusinessStatus status,
        String suspensionReason,
        Instant updatedAt,
        boolean ownBusiness
) {}
