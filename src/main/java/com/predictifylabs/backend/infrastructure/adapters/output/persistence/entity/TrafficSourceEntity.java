package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Traffic sources entity matching PostgreSQL traffic_sources table
 */
@Entity
@Table(name = "traffic_sources", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "source" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(nullable = false)
    @Builder.Default
    private Integer visits = 0;

    @Column(precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer conversions = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
