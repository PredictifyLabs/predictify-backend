package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Saved events join table matching PostgreSQL saved_events table
 */
@Entity
@Table(name = "saved_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SavedEventEntity.SavedEventId.class)
public class SavedEventEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    // Composite Key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedEventId implements Serializable {
        private UUID user;
        private UUID event;
    }
}
