package com.directoriocristiano.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false)
    private int honesty;

    @Column(nullable = false)
    private int quality;

    @Column(nullable = false)
    private int punctuality;

    @Column(nullable = false)
    private int kindness;

    @Column(name = "verified_client", nullable = false)
    private boolean verifiedClient;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
