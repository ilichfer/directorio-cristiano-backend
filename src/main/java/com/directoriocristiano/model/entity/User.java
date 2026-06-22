package com.directoriocristiano.model.entity;

import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.model.enums.VerificationStep;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "pastoral_verification", nullable = false)
    private boolean pastoralVerification;

    private String church;

    @Column(name = "pastor_name")
    private String pastorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_step", nullable = false)
    private VerificationStep verificationStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
