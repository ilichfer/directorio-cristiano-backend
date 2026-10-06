package com.directoriocristiano.repository;

import com.directoriocristiano.model.entity.ModerationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModerationEventRepository extends JpaRepository<ModerationEvent, UUID> {

    List<ModerationEvent> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}
