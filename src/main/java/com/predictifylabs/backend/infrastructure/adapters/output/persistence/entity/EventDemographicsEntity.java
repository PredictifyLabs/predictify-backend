package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event demographics entity matching PostgreSQL event_demographics table
 */
@Entity
@Table(name = "event_demographics", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id",
        "demographic_type", "label" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDemographicsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(name = "demographic_type", nullable = false, length = 50)
    private String demographicType; // 'age_group', 'location', 'industry', 'gender'

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private Integer value = 0;

    @Column(precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal percentage = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
