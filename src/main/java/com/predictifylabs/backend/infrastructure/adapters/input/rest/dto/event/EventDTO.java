package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import com.predictifylabs.backend.domain.model.EventCategory;
import com.predictifylabs.backend.domain.model.EventStatus;
import com.predictifylabs.backend.domain.model.EventType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Event listing and details
 */
@Builder
public record EventDTO(
    UUID id,
    String title,
    String slug,
    String description,
    String shortDescription,
    
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    String timezone,
    
    EventCategory category,
    EventType type,
    EventStatus status,
    
    String imageUrl,
    
    Integer capacity,
    Integer interestedCount,
    Integer registeredCount,
    Integer attendeesCount,
    Integer viewsCount,
    
    BigDecimal price,
    String currency,
    Boolean isFree,
    
    Boolean isFeatured,
    Boolean isTrending,
    Boolean isNew,
    
    OffsetDateTime publishedAt,
    OffsetDateTime createdAt,
    
    OrganizerDTO organizer,
    EventLocationDTO location
) {}
