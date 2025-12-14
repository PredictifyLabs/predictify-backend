package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User preferences entity matching PostgreSQL user_preferences table
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    // Notificaciones
    @Column(name = "notify_email", nullable = false)
    @Builder.Default
    private Boolean notifyEmail = true;

    @Column(name = "notify_push", nullable = false)
    @Builder.Default
    private Boolean notifyPush = true;

    @Column(name = "notify_event_reminders", nullable = false)
    @Builder.Default
    private Boolean notifyEventReminders = true;

    @Column(name = "notify_new_events", nullable = false)
    @Builder.Default
    private Boolean notifyNewEvents = true;

    @Column(name = "notify_event_updates", nullable = false)
    @Builder.Default
    private Boolean notifyEventUpdates = true;

    // Privacidad
    @Column(name = "show_profile", nullable = false)
    @Builder.Default
    private Boolean showProfile = true;

    @Column(name = "show_attended_events", nullable = false)
    @Builder.Default
    private Boolean showAttendedEvents = false;

    @Column(name = "show_saved_events", nullable = false)
    @Builder.Default
    private Boolean showSavedEvents = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
