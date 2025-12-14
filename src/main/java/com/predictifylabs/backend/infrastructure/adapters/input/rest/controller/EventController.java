package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.CreateEventDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.EventDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.UpdateEventDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for event management
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all upcoming events")
    public ResponseEntity<List<EventDTO>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming published events")
    public ResponseEntity<List<EventDTO>> getUpcoming() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured events")
    public ResponseEntity<List<EventDTO>> getFeaturedEvents() {
        return ResponseEntity.ok(eventService.getFeaturedEvents());
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending events")
    public ResponseEntity<List<EventDTO>> getTrendingEvents() {
        return ResponseEntity.ok(eventService.getTrendingEvents());
    }

    @GetMapping("/search")
    @Operation(summary = "Search events by keyword")
    public ResponseEntity<List<EventDTO>> searchEvents(@RequestParam String keyword) {
        return ResponseEntity.ok(eventService.searchEvents(keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<EventDTO> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get event by slug")
    public ResponseEntity<EventDTO> getEventBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(eventService.getEventBySlug(slug));
    }

    @GetMapping("/my-events")
    @Operation(summary = "Get events created by current user (organizer)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventDTO>> getMyEvents(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(eventService.getEventsByOrganizerUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDTO> createEvent(
            @RequestBody @Valid CreateEventDTO dto,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var created = eventService.createEvent(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateEventDTO dto,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var updated = eventService.updateEvent(id, dto, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        eventService.deleteEvent(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDTO> publishEvent(
            @PathVariable UUID id,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var published = eventService.publishEvent(id, userId);
        return ResponseEntity.ok(published);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDTO> cancelEvent(
            @PathVariable UUID id,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var cancelled = eventService.cancelEvent(id, userId);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Extract user ID from authentication
     */
    private UUID extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
