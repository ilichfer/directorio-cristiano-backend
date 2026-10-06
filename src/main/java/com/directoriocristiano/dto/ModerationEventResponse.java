package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.ModerationEvent;
import com.directoriocristiano.model.enums.ModerationAction;

import java.time.Instant;
import java.util.UUID;

/** Entrada del historial de moderación de un negocio (FR-024). */
public record ModerationEventResponse(
        ModerationAction action,
        String actor,
        String reason,
        UUID changeRequestId,
        Instant createdAt
) {
    public static ModerationEventResponse from(ModerationEvent event) {
        return new ModerationEventResponse(
                event.getAction(),
                event.getActor().getDisplayName(),
                event.getReason(),
                event.getChangeRequest() == null ? null : event.getChangeRequest().getId(),
                event.getCreatedAt());
    }
}
