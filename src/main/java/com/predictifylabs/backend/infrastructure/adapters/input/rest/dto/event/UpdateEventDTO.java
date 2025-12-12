package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import com.predictifylabs.backend.domain.model.EventCategory;
import com.predictifylabs.backend.domain.model.EventStatus;
import com.predictifylabs.backend.domain.model.EventType;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for updating an existing event
 */
public record UpdateEventDTO(
    String title,
    String description,
    String shortDescription,
    EventCategory category,
    EventType type,
    EventStatus status,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    String timezone,
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,
    BigDecimal price,
    String currency,
    Boolean isFree,
    String imageUrl,
    Boolean isFeatured,
    Boolean isTrending,
    CreateEventLocationDTO location
) {}
