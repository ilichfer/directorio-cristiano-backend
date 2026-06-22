package com.directoriocristiano.repository;

import com.directoriocristiano.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
}
