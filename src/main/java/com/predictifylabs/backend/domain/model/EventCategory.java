package com.predictifylabs.backend.domain.model;

/**
 * Event category matching PostgreSQL enum event_category
 * Maps to: CREATE TYPE event_category AS ENUM (
 * 'conference', 'hackathon', 'workshop', 'meetup',
 * 'networking', 'bootcamp', 'webinar'
 * )
 */
public enum EventCategory {
    CONFERENCE, // Professional conferences
    HACKATHON, // Coding competitions and hackathons
    WORKSHOP, // Educational workshops
    MEETUP, // Informal meetups and gatherings
    NETWORKING, // Networking events
    BOOTCAMP, // Intensive training bootcamps
    WEBINAR // Online seminars
}
