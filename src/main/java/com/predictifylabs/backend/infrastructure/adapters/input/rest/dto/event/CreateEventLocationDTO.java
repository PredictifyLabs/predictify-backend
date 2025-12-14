package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import com.predictifylabs.backend.domain.model.LocationType;

import java.math.BigDecimal;

/**
 * DTO for creating/updating event location
 */
public record CreateEventLocationDTO(
    LocationType type,
    String venueName,
    String address,
    String city,
    String state,
    String country,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude,
    String virtualUrl,
    String virtualPlatform,
    String instructions
) {}
