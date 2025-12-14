package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event interested users join table matching PostgreSQL event_interested table
 */
@Entity
@Table(name = "event_interested")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EventInterestedEntity.EventInterestedId.class)
public class EventInterestedEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    // Composite Key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventInterestedId implements Serializable {
        private UUID event;
        private UUID user;
    }
}
