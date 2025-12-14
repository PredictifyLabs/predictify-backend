package com.predictifylabs.backend.domain.model;

/**
 * User roles matching PostgreSQL enum user_role
 * Maps to: CREATE TYPE user_role AS ENUM ('attendee', 'organizer', 'admin')
 */
public enum Role {
    ATTENDEE, // Regular user who attends events
    ORGANIZER, // User who can create and manage events
    ADMIN // System administrator with full access
}
