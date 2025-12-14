package com.predictifylabs.backend.domain.model;

/**
 * Event type matching PostgreSQL enum event_type
 * Maps to: CREATE TYPE event_type AS ENUM ('presencial', 'virtual', 'hibrido')
 */
public enum EventType {
    PRESENCIAL, // In-person event
    VIRTUAL, // Online event
    HIBRIDO // Hybrid event (both in-person and online)
}
