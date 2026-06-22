package com.directoriocristiano.dto;

import com.directoriocristiano.model.entity.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID businessId,
        UUID userId,
        String authorName,
        int rating,
        String comment,
        int honesty,
        int quality,
        int punctuality,
        int kindness,
        boolean verifiedClient,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBusiness().getId(),
                review.getUser() != null ? review.getUser().getId() : null,
                review.getAuthorName(),
                review.getRating(),
                review.getComment(),
                review.getHonesty(),
                review.getQuality(),
                review.getPunctuality(),
                review.getKindness(),
                review.isVerifiedClient(),
                review.getCreatedAt()
        );
    }
}
