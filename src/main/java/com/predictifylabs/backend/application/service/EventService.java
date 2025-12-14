package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.domain.model.EventStatus;
import com.predictifylabs.backend.domain.model.LocationType;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.*;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventLocationEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for event management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;

    /**
     * Get all upcoming published events
     */
    public List<EventDTO> getUpcomingEvents() {
        LocalDate today = LocalDate.now();
        var events = eventRepository.findUpcomingEvents(today);
        return events.stream().map(this::toDTO).toList();
    }

    /**
     * Get all events (for admin)
     */
    public List<EventDTO> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toDTO).toList();
    }

    /**
     * Get featured events
     */
    public List<EventDTO> getFeaturedEvents() {
        return eventRepository.findFeaturedEvents().stream().map(this::toDTO).toList();
    }

    /**
     * Get trending events
     */
    public List<EventDTO> getTrendingEvents() {
        return eventRepository.findTrendingEvents().stream().map(this::toDTO).toList();
    }

    /**
     * Search events by keyword
     */
    public List<EventDTO> searchEvents(String keyword) {
        return eventRepository.searchByKeyword(keyword).stream().map(this::toDTO).toList();
    }

    /**
     * Get event by ID
     */
    public EventDTO getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    /**
     * Get event by slug
     */
    public EventDTO getEventBySlug(String slug) {
        return eventRepository.findBySlug(slug)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Event not found with slug: " + slug));
    }

    /**
     * Get events by organizer
     */
    public List<EventDTO> getEventsByOrganizer(UUID organizerId) {
        return eventRepository.findByOrganizer(organizerId).stream().map(this::toDTO).toList();
    }

    /**
     * Get events by organizer user ID
     */
    public List<EventDTO> getEventsByOrganizerUserId(UUID userId) {
        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Organizer not found for user: " + userId));
        return eventRepository.findByOrganizer(organizer.getId()).stream().map(this::toDTO).toList();
    }

    /**
     * Create a new event
     */
    @Transactional
    public EventDTO createEvent(CreateEventDTO dto, UUID userId) {
        log.info("Creating event '{}' for user {}", dto.title(), userId);

        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User is not an organizer. Please create an organizer profile first."));

        var event = EventEntity.builder()
                .organizer(organizer)
                .title(dto.title())
                .slug(generateSlug(dto.title()))
                .description(dto.description())
                .shortDescription(dto.shortDescription())
                .category(dto.category())
                .type(dto.type())
                .status(EventStatus.DRAFT)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .timezone(dto.timezone() != null ? dto.timezone() : "UTC")
                .capacity(dto.capacity())
                .price(dto.price())
                .currency(dto.currency() != null ? dto.currency() : "USD")
                .isFree(dto.isFree() != null ? dto.isFree() : true)
                .imageUrl(dto.imageUrl())
                .build();

        // Handle location if provided
        if (dto.location() != null) {
            var location = createLocationEntity(dto.location(), event);
            event.setLocation(location);
        }

        var saved = eventRepository.save(event);
        
        // Update organizer event count
        organizer.setEventsCount(organizer.getEventsCount() + 1);
        organizerRepository.save(organizer);

        log.info("Event created with ID: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Update an existing event
     */
    @Transactional
    public EventDTO updateEvent(UUID eventId, UpdateEventDTO dto, UUID userId) {
        log.info("Updating event {} for user {}", eventId, userId);

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        // Verify ownership
        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User is not an organizer"));

        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("You are not authorized to update this event");
        }

        // Update fields if provided
        if (dto.title() != null) event.setTitle(dto.title());
        if (dto.description() != null) event.setDescription(dto.description());
        if (dto.shortDescription() != null) event.setShortDescription(dto.shortDescription());
        if (dto.category() != null) event.setCategory(dto.category());
        if (dto.type() != null) event.setType(dto.type());
        if (dto.status() != null) {
            updateEventStatus(event, dto.status());
        }
        if (dto.startDate() != null) event.setStartDate(dto.startDate());
        if (dto.endDate() != null) event.setEndDate(dto.endDate());
        if (dto.startTime() != null) event.setStartTime(dto.startTime());
        if (dto.endTime() != null) event.setEndTime(dto.endTime());
        if (dto.timezone() != null) event.setTimezone(dto.timezone());
        if (dto.capacity() != null) event.setCapacity(dto.capacity());
        if (dto.price() != null) event.setPrice(dto.price());
        if (dto.currency() != null) event.setCurrency(dto.currency());
        if (dto.isFree() != null) event.setIsFree(dto.isFree());
        if (dto.imageUrl() != null) event.setImageUrl(dto.imageUrl());
        if (dto.isFeatured() != null) event.setIsFeatured(dto.isFeatured());
        if (dto.isTrending() != null) event.setIsTrending(dto.isTrending());

        // Update location if provided
        if (dto.location() != null) {
            if (event.getLocation() != null) {
                updateLocationEntity(event.getLocation(), dto.location());
            } else {
                var location = createLocationEntity(dto.location(), event);
                event.setLocation(location);
            }
        }

        var saved = eventRepository.save(event);
        log.info("Event updated: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Delete an event
     */
    @Transactional
    public void deleteEvent(UUID eventId, UUID userId) {
        log.info("Deleting event {} for user {}", eventId, userId);

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        // Verify ownership
        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User is not an organizer"));

        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("You are not authorized to delete this event");
        }

        eventRepository.delete(event);
        
        // Update organizer event count
        organizer.setEventsCount(Math.max(0, organizer.getEventsCount() - 1));
        organizerRepository.save(organizer);

        log.info("Event deleted: {}", eventId);
    }

    /**
     * Publish an event
     */
    @Transactional
    public EventDTO publishEvent(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User is not an organizer"));

        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("You are not authorized to publish this event");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(OffsetDateTime.now());
        event.setIsNew(true);

        var saved = eventRepository.save(event);
        log.info("Event published: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Cancel an event
     */
    @Transactional
    public EventDTO cancelEvent(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User is not an organizer"));

        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("You are not authorized to cancel this event");
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setCancelledAt(OffsetDateTime.now());

        var saved = eventRepository.save(event);
        log.info("Event cancelled: {}", saved.getId());
        return toDTO(saved);
    }

    // Helper methods
    private void updateEventStatus(EventEntity event, EventStatus newStatus) {
        event.setStatus(newStatus);
        switch (newStatus) {
            case PUBLISHED -> event.setPublishedAt(OffsetDateTime.now());
            case CANCELLED -> event.setCancelledAt(OffsetDateTime.now());
            case COMPLETED -> event.setCompletedAt(OffsetDateTime.now());
            default -> {}
        }
    }

    private EventLocationEntity createLocationEntity(CreateEventLocationDTO dto, EventEntity event) {
        return EventLocationEntity.builder()
                .event(event)
                .type(dto.type() != null ? dto.type() : LocationType.PHYSICAL)
                .venue(dto.venueName())
                .address(dto.address())
                .city(dto.city())
                .country(dto.country())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .virtualLink(dto.virtualUrl())
                .virtualPlatform(dto.virtualPlatform())
                .build();
    }

    private void updateLocationEntity(EventLocationEntity location, CreateEventLocationDTO dto) {
        if (dto.type() != null) location.setType(dto.type());
        if (dto.venueName() != null) location.setVenue(dto.venueName());
        if (dto.address() != null) location.setAddress(dto.address());
        if (dto.city() != null) location.setCity(dto.city());
        if (dto.country() != null) location.setCountry(dto.country());
        if (dto.latitude() != null) location.setLatitude(dto.latitude());
        if (dto.longitude() != null) location.setLongitude(dto.longitude());
        if (dto.virtualUrl() != null) location.setVirtualLink(dto.virtualUrl());
        if (dto.virtualPlatform() != null) location.setVirtualPlatform(dto.virtualPlatform());
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private EventDTO toDTO(EventEntity event) {
        OrganizerDTO organizerDTO = null;
        if (event.getOrganizer() != null) {
            var org = event.getOrganizer();
            organizerDTO = OrganizerDTO.builder()
                    .id(org.getId())
                    .displayName(org.getDisplayName())
                    .avatar(org.getAvatar())
                    .isVerified(org.getIsVerified())
                    .eventsCount(org.getEventsCount())
                    .rating(org.getRating())
                    .build();
        }

        EventLocationDTO locationDTO = null;
        if (event.getLocation() != null) {
            var loc = event.getLocation();
            locationDTO = EventLocationDTO.builder()
                    .type(loc.getType())
                    .venue(loc.getVenue())
                    .address(loc.getAddress())
                    .city(loc.getCity())
                    .country(loc.getCountry())
                    .latitude(loc.getLatitude())
                    .longitude(loc.getLongitude())
                    .virtualLink(loc.getVirtualLink())
                    .virtualPlatform(loc.getVirtualPlatform())
                    .build();
        }

        return EventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .description(event.getDescription())
                .shortDescription(event.getShortDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .timezone(event.getTimezone())
                .category(event.getCategory())
                .type(event.getType())
                .status(event.getStatus())
                .imageUrl(event.getImageUrl())
                .capacity(event.getCapacity())
                .interestedCount(event.getInterestedCount())
                .registeredCount(event.getRegisteredCount())
                .attendeesCount(event.getAttendeesCount())
                .viewsCount(event.getViewsCount())
                .price(event.getPrice())
                .currency(event.getCurrency())
                .isFree(event.getIsFree())
                .isFeatured(event.getIsFeatured())
                .isTrending(event.getIsTrending())
                .isNew(event.getIsNew())
                .publishedAt(event.getPublishedAt())
                .createdAt(event.getCreatedAt())
                .organizer(organizerDTO)
                .location(locationDTO)
                .build();
    }
}
