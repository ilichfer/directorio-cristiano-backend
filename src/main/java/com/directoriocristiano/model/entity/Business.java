package com.directoriocristiano.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String zone;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String slogan;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false)
    private boolean featured;

    @ElementCollection
    @CollectionTable(name = "business_values", joinColumns = @JoinColumn(name = "business_id"))
    @Column(name = "value")
    private List<String> values = new ArrayList<>();

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "contact_whatsapp", nullable = false)
    private String contactWhatsapp;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_website")
    private String contactWebsite;

    @Column(name = "contact_address", columnDefinition = "TEXT", nullable = false)
    private String contactAddress;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ServiceItem> services = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (rating == null) {
            rating = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
