package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.CreateOrganizerDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.OrganizerProfileDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.OrganizerEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.OrganizerRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for organizer management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;

    /**
     * Get organizer by ID
     */
    public OrganizerProfileDTO getOrganizerById(UUID organizerId) {
        return organizerRepository.findById(organizerId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Organizer not found with id: " + organizerId));
    }

    /**
     * Get organizer by user ID
     */
    public OrganizerProfileDTO getOrganizerByUserId(UUID userId) {
        return organizerRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Organizer not found for user: " + userId));
    }

    /**
     * Check if user is an organizer
     */
    public boolean isUserOrganizer(UUID userId) {
        return organizerRepository.existsByUserId(userId);
    }

    /**
     * Create organizer profile
     */
    @Transactional
    public OrganizerProfileDTO createOrganizer(UUID userId, CreateOrganizerDTO dto) {
        log.info("Creating organizer profile for user {}", userId);

        // Check if already an organizer
        if (organizerRepository.existsByUserId(userId)) {
            throw new RuntimeException("User already has an organizer profile");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        var organizer = OrganizerEntity.builder()
                .user(user)
                .displayName(dto.displayName())
                .avatar(dto.avatar())
                .bio(dto.bio())
                .email(dto.email() != null ? dto.email() : user.getEmail())
                .website(dto.website())
                .build();

        var saved = organizerRepository.save(organizer);
        log.info("Organizer profile created with ID: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Update organizer profile
     */
    @Transactional
    public OrganizerProfileDTO updateOrganizer(UUID userId, CreateOrganizerDTO dto) {
        log.info("Updating organizer profile for user {}", userId);

        var organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Organizer not found for user: " + userId));

        if (dto.displayName() != null) organizer.setDisplayName(dto.displayName());
        if (dto.avatar() != null) organizer.setAvatar(dto.avatar());
        if (dto.bio() != null) organizer.setBio(dto.bio());
        if (dto.email() != null) organizer.setEmail(dto.email());
        if (dto.website() != null) organizer.setWebsite(dto.website());

        var saved = organizerRepository.save(organizer);
        log.info("Organizer profile updated for user {}", userId);
        return toDTO(saved);
    }

    /**
     * Get all organizers
     */
    public List<OrganizerProfileDTO> getAllOrganizers() {
        return organizerRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    // Helper methods
    private OrganizerProfileDTO toDTO(OrganizerEntity organizer) {
        return OrganizerProfileDTO.builder()
                .id(organizer.getId())
                .userId(organizer.getUser().getId())
                .displayName(organizer.getDisplayName())
                .avatar(organizer.getAvatar())
                .bio(organizer.getBio())
                .email(organizer.getEmail())
                .website(organizer.getWebsite())
                .isVerified(organizer.getIsVerified())
                .verifiedAt(organizer.getVerifiedAt())
                .eventsCount(organizer.getEventsCount())
                .averageAttendanceRate(organizer.getAverageAttendanceRate())
                .totalAttendees(organizer.getTotalAttendees())
                .rating(organizer.getRating())
                .ratingCount(organizer.getRatingCount())
                .createdAt(organizer.getCreatedAt())
                .build();
    }
}
