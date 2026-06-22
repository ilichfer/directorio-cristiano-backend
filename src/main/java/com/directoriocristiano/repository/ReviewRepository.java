package com.directoriocristiano.repository;

import com.directoriocristiano.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.business.id = :businessId")
    Double averageRatingByBusinessId(@Param("businessId") UUID businessId);

    long countByBusinessId(UUID businessId);
}
