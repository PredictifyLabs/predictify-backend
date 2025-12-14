package com.predictifylabs.backend.domain.model;

/**
 * Location type matching PostgreSQL enum location_type
 * Maps to: CREATE TYPE location_type AS ENUM ('physical', 'virtual', 'hybrid')
 */
public enum LocationType {
    PHYSICAL, // Physical venue location
    VIRTUAL, // Online/virtual location
    HYBRID // Both physical and virtual
}
