package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction;

import com.predictifylabs.backend.domain.model.FactorImpact;
import com.predictifylabs.backend.domain.model.FactorType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for prediction factor data
 */
@Builder
public record PredictionFactorDTO(
    UUID id,
    String name,
    FactorType type,
    FactorImpact impact,
    BigDecimal weight,
    BigDecimal score,
    String description
) {}
