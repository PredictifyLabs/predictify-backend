package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import com.predictifylabs.backend.domain.model.EventCategory;
import com.predictifylabs.backend.domain.model.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a new event
 */
public record CreateEventDTO(
    @NotBlank(message = "Title is required")
    String title,
    
    @NotBlank(message = "Description is required")
    String description,
    
    String shortDescription,
    
    @NotNull(message = "Category is required")
    EventCategory category,
    
    @NotNull(message = "Type is required")
    EventType type,
    
    @NotNull(message = "Start date is required")
    LocalDate startDate,
    
    LocalDate endDate,
    
    @NotNull(message = "Start time is required")
    LocalTime startTime,
    
    LocalTime endTime,
    
    String timezone,
    
    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,
    
    BigDecimal price,
    
    String currency,
    
    Boolean isFree,
    
    String imageUrl,
    
    CreateEventLocationDTO location
) {}
