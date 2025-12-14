package com.predictifylabs.backend.domain.model;

/**
 * Prediction level matching PostgreSQL enum prediction_level
 * Maps to: CREATE TYPE prediction_level AS ENUM ('high', 'medium', 'low')
 */
public enum PredictionLevel {
    HIGH, // High confidence prediction
    MEDIUM, // Medium confidence prediction
    LOW // Low confidence prediction
}
