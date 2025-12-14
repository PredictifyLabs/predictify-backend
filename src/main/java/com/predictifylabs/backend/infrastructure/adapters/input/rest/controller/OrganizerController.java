package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventService;
import com.predictifylabs.backend.application.service.OrganizerService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.EventDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.CreateOrganizerDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.OrganizerProfileDTO;
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
 * REST Controller for organizer management
 */
@RestController
@RequestMapping("/api/v1/organizers")
@RequiredArgsConstructor
@Tag(name = "Organizers", description = "Organizer profile management endpoints")
public class OrganizerController {

    private final OrganizerService organizerService;
    private final EventService eventService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all organizers")
    public ResponseEntity<List<OrganizerProfileDTO>> getAllOrganizers() {
        return ResponseEntity.ok(organizerService.getAllOrganizers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organizer by ID")
    public ResponseEntity<OrganizerProfileDTO> getOrganizerById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizerService.getOrganizerById(id));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get events by organizer")
    public ResponseEntity<List<EventDTO>> getOrganizerEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's organizer profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizerProfileDTO> getMyOrganizerProfile(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(organizerService.getOrganizerByUserId(userId));
    }

    @GetMapping("/me/check")
    @Operation(summary = "Check if current user is an organizer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isCurrentUserOrganizer(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(organizerService.isUserOrganizer(userId));
    }

    @PostMapping
    @Operation(summary = "Create organizer profile for current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizerProfileDTO> createOrganizerProfile(
            @RequestBody @Valid CreateOrganizerDTO dto,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var created = organizerService.createOrganizer(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's organizer profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizerProfileDTO> updateOrganizerProfile(
            @RequestBody @Valid CreateOrganizerDTO dto,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var updated = organizerService.updateOrganizer(userId, dto);
        return ResponseEntity.ok(updated);
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
