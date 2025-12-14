package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, UUID> {

    Optional<EventRegistrationEntity> findByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventRegistrationEntity> findByUserId(UUID userId);

    List<EventRegistrationEntity> findByEventId(UUID eventId);

    @Query("SELECT er FROM EventRegistrationEntity er WHERE er.event.id = :eventId AND er.status = :status")
    List<EventRegistrationEntity> findByEventIdAndStatus(@Param("eventId") UUID eventId,
            @Param("status") String status);

    @Query("SELECT COUNT(er) FROM EventRegistrationEntity er WHERE er.event.id = :eventId AND er.attended = true")
    Long countAttendedByEventId(@Param("eventId") UUID eventId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
}
