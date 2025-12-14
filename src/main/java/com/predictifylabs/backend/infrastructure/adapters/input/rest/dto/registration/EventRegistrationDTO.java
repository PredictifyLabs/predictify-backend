package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.registration;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for event registration data
 */
@Builder
public record EventRegistrationDTO(
    UUID id,
    UUID eventId,
    String eventTitle,
    String eventSlug,
    UUID userId,
    String userName,
    String status,
    String ticketCode,
    Boolean attended,
    OffsetDateTime attendedAt,
    BigDecimal amountPaid,
    String paymentStatus,
    OffsetDateTime registeredAt,
    OffsetDateTime cancelledAt
) {}
