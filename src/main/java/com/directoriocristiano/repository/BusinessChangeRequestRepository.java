package com.directoriocristiano.repository;

import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.enums.ChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessChangeRequestRepository extends JpaRepository<BusinessChangeRequest, UUID> {

    /** Como máximo hay una pendiente por negocio (índice único parcial en V7). */
    Optional<BusinessChangeRequest> findByBusinessIdAndStatus(UUID businessId, ChangeRequestStatus status);

    Optional<BusinessChangeRequest> findFirstByBusinessIdAndStatusOrderByReviewedAtDesc(
            UUID businessId, ChangeRequestStatus status);

    /** Bandeja del moderador: la más antigua primero (FR-016). */
    Page<BusinessChangeRequest> findByStatusOrderBySubmittedAtAsc(ChangeRequestStatus status, Pageable pageable);

    List<BusinessChangeRequest> findBySubmittedByIdAndStatus(UUID userId, ChangeRequestStatus status);
}
