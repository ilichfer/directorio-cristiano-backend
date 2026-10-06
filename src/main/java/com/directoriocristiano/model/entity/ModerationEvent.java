package com.directoriocristiano.model.entity;

import com.directoriocristiano.model.enums.ModerationAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Registro inmutable de cada acción de moderación sobre un negocio (FR-024). */
@Entity
@Table(name = "moderation_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", updatable = false)
    private BusinessChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ModerationAction action;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
