package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event-Tag join table entity matching PostgreSQL event_tags table
 */
@Entity
@Table(name = "event_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EventTagEntity.EventTagId.class)
public class EventTagEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    // Composite Key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventTagId implements Serializable {
        private UUID event;
        private UUID tag;
    }
}
