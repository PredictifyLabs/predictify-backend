package com.predictifylabs.backend.domain.model;

/**
 * Prediction trend matching PostgreSQL enum prediction_trend
 * Maps to: CREATE TYPE prediction_trend AS ENUM ('up', 'down', 'stable')
 */
public enum PredictionTrend {
    UP, // Trend is increasing
    DOWN, // Trend is decreasing
    STABLE // Trend is stable
}
