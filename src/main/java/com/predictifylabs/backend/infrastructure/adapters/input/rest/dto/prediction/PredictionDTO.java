package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction;

import com.predictifylabs.backend.domain.model.PredictionLevel;
import com.predictifylabs.backend.domain.model.PredictionTrend;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for event prediction data
 */
@Builder
public record PredictionDTO(
    UUID id,
    UUID eventId,
    Short probability,
    PredictionLevel level,
    Short confidence,
    Integer estimatedMin,
    Integer estimatedMax,
    Integer estimatedExpected,
    PredictionTrend trend,
    BigDecimal trendChange,
    OffsetDateTime calculatedAt,
    List<PredictionFactorDTO> factors
) {}
