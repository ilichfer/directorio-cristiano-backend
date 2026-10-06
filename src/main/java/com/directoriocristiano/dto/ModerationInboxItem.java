package com.directoriocristiano.dto;

import com.directoriocristiano.model.enums.ChangeRequestType;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la bandeja del moderador (FR-016). {@code overdue}: lleva 3 días o más esperando
 * (SC-003). {@code ownRequest}: el moderador es el dueño y no puede decidirla (FR-019).
 */
public record ModerationInboxItem(
        UUID id,
        ChangeRequestType type,
        UUID businessId,
        String businessName,
        String category,
        String ownerName,
        Instant submittedAt,
        long daysWaiting,
        boolean overdue,
        boolean ownRequest
) {}
