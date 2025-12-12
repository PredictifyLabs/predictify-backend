package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for organizer profile data
 */
@Builder
public record OrganizerProfileDTO(
    UUID id,
    UUID userId,
    String displayName,
    String avatar,
    String bio,
    String email,
    String website,
    Boolean isVerified,
    OffsetDateTime verifiedAt,
    Integer eventsCount,
    BigDecimal averageAttendanceRate,
    Integer totalAttendees,
    BigDecimal rating,
    Integer ratingCount,
    OffsetDateTime createdAt
) {}
