package com.directoriocristiano.model.entity;

import com.directoriocristiano.model.enums.ChangeRequestStatus;
import com.directoriocristiano.model.enums.ChangeRequestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Propuesta de publicación (negocio nuevo) o de edición (negocio publicado). La versión publicada
 * vive en {@link Business} y no cambia hasta que la solicitud se aprueba (FR-010).
 */
@Entity
@Table(name = "business_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeRequestStatus status;

    /** Estado propuesto de los campos que requieren aprobación (ver data-model.md). */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> proposed = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** Bloqueo optimista: detecta si el dueño editó mientras un moderador revisaba (FR-020). */
    @Version
    @Column(nullable = false)
    private int version;
}
