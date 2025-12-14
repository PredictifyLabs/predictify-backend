package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event;

import com.predictifylabs.backend.domain.model.LocationType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EventLocationDTO(
        LocationType type,
        String address,
        String city,
        String country,
        String venue,
        BigDecimal latitude,
        BigDecimal longitude,
        String virtualLink,
        String virtualPlatform) {
}
