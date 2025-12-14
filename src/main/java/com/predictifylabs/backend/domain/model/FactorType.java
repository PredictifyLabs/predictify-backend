package com.predictifylabs.backend.domain.model;

/**
 * Factor type matching PostgreSQL enum factor_type
 * Maps to: CREATE TYPE factor_type AS ENUM ('positive', 'negative', 'neutral')
 */
public enum FactorType {
    POSITIVE, // Factor that positively impacts attendance
    NEGATIVE, // Factor that negatively impacts attendance
    NEUTRAL // Factor with neutral impact
}
