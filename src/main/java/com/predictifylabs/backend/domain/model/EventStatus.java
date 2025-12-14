package com.predictifylabs.backend.domain.model;

/**
 * Event status matching PostgreSQL enum event_status
 * Maps to: CREATE TYPE event_status AS ENUM ('draft', 'published', 'cancelled',
 * 'completed')
 */
public enum EventStatus {
    DRAFT, // Event is being created/edited
    PUBLISHED, // Event is live and visible to users
    CANCELLED, // Event was cancelled
    COMPLETED // Event has finished
}
