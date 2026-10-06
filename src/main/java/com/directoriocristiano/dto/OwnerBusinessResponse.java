package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.enums.ChangeRequestType;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vista del dueño: la ficha completa más su estado de moderación (contracts/owner-businesses.md). */
public record OwnerBusinessResponse(
        @JsonUnwrapped BusinessResponse business,
        Instant publishedAt,
        String suspensionReason,
        PendingRequest pendingRequest,
        Rejection lastRejection,
        List<String> appliedImmediately
) {
    public static OwnerBusinessResponse from(Business business,
                                             BusinessChangeRequest pending,
                                             BusinessChangeRequest lastRejected,
                                             List<String> appliedImmediately) {
        return new OwnerBusinessResponse(
                BusinessResponse.from(business),
                business.getPublishedAt(),
                business.getSuspensionReason(),
                pending == null ? null : new PendingRequest(
                        pending.getId(), pending.getType(), pending.getSubmittedAt(), pending.getProposed()),
                lastRejected == null ? null : new Rejection(
                        lastRejected.getId(), lastRejected.getRejectionReason(),
                        lastRejected.getReviewedAt(), lastRejected.getProposed()),
                appliedImmediately == null ? List.of() : appliedImmediately
        );
    }

    public record PendingRequest(UUID id, ChangeRequestType type, Instant submittedAt, Map<String, Object> proposed) {}

    public record Rejection(UUID id, String reason, Instant reviewedAt, Map<String, Object> proposed) {}
}
