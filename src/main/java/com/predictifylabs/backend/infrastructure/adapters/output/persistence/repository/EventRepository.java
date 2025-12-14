package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.domain.model.EventStatus;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findBySlug(String slug);

    List<EventEntity> findByStatus(EventStatus status);

    @Query("SELECT e FROM EventEntity e WHERE e.status = 'PUBLISHED' AND e.startDate >= :currentDate ORDER BY e.startDate ASC")
    List<EventEntity> findUpcomingEvents(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT e FROM EventEntity e WHERE e.organizer.id = :organizerId ORDER BY e.createdAt DESC")
    List<EventEntity> findByOrganizer(@Param("organizerId") UUID organizerId);

    @Query("SELECT e FROM EventEntity e WHERE e.status = 'PUBLISHED' AND e.isFeatured = true ORDER BY e.startDate ASC")
    List<EventEntity> findFeaturedEvents();

    @Query("SELECT e FROM EventEntity e WHERE e.status = 'PUBLISHED' AND e.isTrending = true ORDER BY e.viewsCount DESC")
    List<EventEntity> findTrendingEvents();

    @Query("SELECT e FROM EventEntity e WHERE e.status = 'PUBLISHED' AND LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<EventEntity> searchByKeyword(@Param("keyword") String keyword);
}
