package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EventPredictionRepository extends JpaRepository<EventPredictionEntity, UUID> {

    @Query("SELECT ep FROM EventPredictionEntity ep WHERE ep.event.id = :eventId ORDER BY ep.calculatedAt DESC LIMIT 1")
    Optional<EventPredictionEntity> findLatestByEventId(@Param("eventId") UUID eventId);
}
