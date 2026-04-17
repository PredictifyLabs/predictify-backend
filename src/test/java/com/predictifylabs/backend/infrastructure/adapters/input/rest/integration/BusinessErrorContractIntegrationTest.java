package com.predictifylabs.backend.infrastructure.adapters.input.rest.integration;

import com.predictifylabs.backend.domain.model.EventCategory;
import com.predictifylabs.backend.domain.model.EventStatus;
import com.predictifylabs.backend.domain.model.EventType;
import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorCodes;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventRegistrationEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.OrganizerEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRegistrationRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.OrganizerRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class BusinessErrorContractIntegrationTest {

    private static final String ORGANIZER_OWNER_EMAIL = "owner@predictify.dev";
    private static final String ATTENDEE_EMAIL = "attendee@predictify.dev";
    private static final String OTHER_ORGANIZER_EMAIL = "other-organizer@predictify.dev";
    private static final String ADMIN_EMAIL = "admin@predictify.dev";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Test
    void missingEventByIdShouldReturn404AndNot500() throws Exception {
        UUID missingEventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/{id}", missingEventId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.RESOURCE_NOT_FOUND))
                .andExpect(jsonPath("$.path").value("/api/v1/events/" + missingEventId));
    }

    @Test
    void duplicateEventRegistrationShouldReturn409AndNot500() throws Exception {
        UserEntity organizerUser = userRepository.save(buildUser(ORGANIZER_OWNER_EMAIL, Role.ORGANIZER));
        OrganizerEntity organizer = organizerRepository.save(buildOrganizer(organizerUser));

        UserEntity attendee = userRepository.save(buildUser(ATTENDEE_EMAIL, Role.ATTENDEE));
        EventEntity event = eventRepository.save(buildEvent(organizer, "event-slug-" + UUID.randomUUID()));

        eventRegistrationRepository.save(EventRegistrationEntity.builder()
                .event(event)
                .user(attendee)
                .status("registered")
                .ticketCode("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .registeredAt(OffsetDateTime.now())
                .build());

        mockMvc.perform(post("/api/v1/events/{eventId}/register", event.getId())
                        .with(user(ATTENDEE_EMAIL).roles("USER")))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.BUSINESS_CONFLICT))
                .andExpect(jsonPath("$.path").value("/api/v1/events/" + event.getId() + "/register"));
    }

    @Test
    void publishingEventFromDifferentOrganizerShouldReturn403AndNot500() throws Exception {
        UserEntity ownerUser = userRepository.save(buildUser(ORGANIZER_OWNER_EMAIL, Role.ORGANIZER));
        OrganizerEntity ownerOrganizer = organizerRepository.save(buildOrganizer(ownerUser));
        EventEntity event = eventRepository.save(buildEvent(ownerOrganizer, "publish-slug-" + UUID.randomUUID()));

        UserEntity anotherOrganizerUser = userRepository.save(buildUser(OTHER_ORGANIZER_EMAIL, Role.ORGANIZER));
        organizerRepository.save(buildOrganizer(anotherOrganizerUser));

        mockMvc.perform(post("/api/v1/events/{id}/publish", event.getId())
                        .with(user(OTHER_ORGANIZER_EMAIL).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_FORBIDDEN))
                .andExpect(jsonPath("$.path").value("/api/v1/events/" + event.getId() + "/publish"));
    }

    @Test
    void adminRoleShouldAccessAdminProtectedEndpointWith2xx() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void userRoleShouldReceive403WithDeterministicContractOnAdminProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(user(ATTENDEE_EMAIL).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_FORBIDDEN))
                .andExpect(jsonPath("$.path").value("/api/v1/users"));
    }

    @Test
    void validationFailureShouldReturn400WithStableCodeAndFieldErrorsMap() throws Exception {
        String invalidRegisterPayload = """
                {
                  \"name\": \"\",
                  \"email\": \"invalid-email\",
                  \"password\": \"123\"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRegisterPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    private UserEntity buildUser(String email, Role role) {
        return UserEntity.builder()
                .name("Test User")
                .email(email)
                .password("hashed-password")
                .role(role)
                .isActive(true)
                .isVerified(true)
                .build();
    }

    private OrganizerEntity buildOrganizer(UserEntity user) {
        return OrganizerEntity.builder()
                .user(user)
                .displayName("Organizer " + UUID.randomUUID())
                .email(user.getEmail())
                .build();
    }

    private EventEntity buildEvent(OrganizerEntity organizer, String slug) {
        return EventEntity.builder()
                .organizer(organizer)
                .title("Predictify Event")
                .slug(slug)
                .description("Event for hardening tests")
                .category(EventCategory.CONFERENCE)
                .type(EventType.PRESENCIAL)
                .status(EventStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(10))
                .startTime(LocalTime.of(10, 0))
                .timezone("UTC")
                .capacity(100)
                .build();
    }
}
