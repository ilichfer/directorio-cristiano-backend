package com.directoriocristiano.dto;

import com.directoriocristiano.model.enums.BusinessStatus;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Detalle de una solicitud para el moderador, con solo los campos que cambian (FR-017). */
public record ChangeRequestDetail(
        UUID id,
        ChangeRequestType type,
        ChangeRequestStatus status,
        int version,
        BusinessRef business,
        UserRef submittedBy,
        Instant submittedAt,
        boolean ownRequest,
        String rejectionReason,
        Instant reviewedAt,
        List<FieldChange> changes
) {
    public record BusinessRef(UUID id, String name, BusinessStatus status) {}

    public record UserRef(UUID id, String displayName) {}

    public record FieldChange(String field, String label, String before, String after) {}
}
