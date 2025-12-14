package com.predictifylabs.backend.domain.model;

/**
 * Factor impact matching PostgreSQL enum factor_impact
 * Maps to: CREATE TYPE factor_impact AS ENUM ('high', 'medium', 'low')
 */
public enum FactorImpact {
    HIGH, // High impact on prediction
    MEDIUM, // Medium impact on prediction
    LOW // Low impact on prediction
}
