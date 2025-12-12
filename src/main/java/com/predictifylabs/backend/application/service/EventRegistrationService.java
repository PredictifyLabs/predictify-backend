package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.registration.EventRegistrationDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventRegistrationEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRegistrationRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for event registration operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /**
     * Register a user to an event
     */
    @Transactional
    public EventRegistrationDTO registerToEvent(UUID eventId, UUID userId) {
        log.info("Registering user {} to event {}", userId, eventId);

        // Check if already registered
        if (registrationRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("User is already registered to this event");
        }

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check capacity
        if (event.getRegisteredCount() >= event.getCapacity()) {
            throw new RuntimeException("Event is at full capacity");
        }

        var registration = EventRegistrationEntity.builder()
                .event(event)
                .user(user)
                .status("registered")
                .ticketCode(generateTicketCode())
                .registeredAt(OffsetDateTime.now())
                .build();

        var saved = registrationRepository.save(registration);

        // Update event registered count
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        log.info("User {} registered to event {} with ticket {}", userId, eventId, saved.getTicketCode());
        return toDTO(saved);
    }

    /**
     * Cancel a registration
     */
    @Transactional
    public void cancelRegistration(UUID eventId, UUID userId) {
        log.info("Cancelling registration for user {} from event {}", userId, eventId);

        var registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        if ("cancelled".equals(registration.getStatus())) {
            throw new RuntimeException("Registration is already cancelled");
        }

        registration.setStatus("cancelled");
        registration.setCancelledAt(OffsetDateTime.now());
        registrationRepository.save(registration);

        // Update event count
        var event = registration.getEvent();
        event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
        eventRepository.save(event);

        log.info("Registration cancelled for user {} from event {}", userId, eventId);
    }

    /**
     * Get registration status for a user and event
     */
    public EventRegistrationDTO getRegistration(UUID eventId, UUID userId) {
        return registrationRepository.findByEventIdAndUserId(eventId, userId)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Check if user is registered to an event
     */
    public boolean isUserRegistered(UUID eventId, UUID userId) {
        return registrationRepository.existsByEventIdAndUserId(eventId, userId);
    }

    /**
     * Get all registrations for a user
     */
    public List<EventRegistrationDTO> getUserRegistrations(UUID userId) {
        return registrationRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get all registrations for an event
     */
    public List<EventRegistrationDTO> getEventRegistrations(UUID eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Mark attendance for a registration
     */
    @Transactional
    public EventRegistrationDTO markAttendance(UUID eventId, UUID userId) {
        log.info("Marking attendance for user {} at event {}", userId, eventId);

        var registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.setAttended(true);
        registration.setAttendedAt(OffsetDateTime.now());
        registration.setStatus("confirmed");

        var saved = registrationRepository.save(registration);

        // Update event attendees count
        var event = registration.getEvent();
        event.setAttendeesCount(event.getAttendeesCount() + 1);
        eventRepository.save(event);

        log.info("Attendance marked for user {} at event {}", userId, eventId);
        return toDTO(saved);
    }

    // Helper methods
    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private EventRegistrationDTO toDTO(EventRegistrationEntity registration) {
        return EventRegistrationDTO.builder()
                .id(registration.getId())
                .eventId(registration.getEvent().getId())
                .eventTitle(registration.getEvent().getTitle())
                .eventSlug(registration.getEvent().getSlug())
                .userId(registration.getUser().getId())
                .userName(registration.getUser().getName())
                .status(registration.getStatus())
                .ticketCode(registration.getTicketCode())
                .attended(registration.getAttended())
                .attendedAt(registration.getAttendedAt())
                .amountPaid(registration.getAmountPaid())
                .paymentStatus(registration.getPaymentStatus())
                .registeredAt(registration.getRegisteredAt())
                .cancelledAt(registration.getCancelledAt())
                .build();
    }
}
