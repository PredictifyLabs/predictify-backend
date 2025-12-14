package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrganizerDTO(
        UUID id,
        String displayName,
        String avatar,
        String bio,
        String email,
        String website,
        Boolean isVerified,
        Integer eventsCount,
        BigDecimal rating) {
}
