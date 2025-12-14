package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.OrganizerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizerRepository extends JpaRepository<OrganizerEntity, UUID> {

    Optional<OrganizerEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
