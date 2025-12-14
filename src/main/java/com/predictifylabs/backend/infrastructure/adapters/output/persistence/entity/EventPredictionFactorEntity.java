package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.FactorImpact;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event prediction factors entity matching PostgreSQL event_prediction_factors
 * table
 */
@Entity
@Table(name = "event_prediction_factors", uniqueConstraints = @UniqueConstraint(columnNames = { "prediction_id",
        "factor_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPredictionFactorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    private EventPredictionEntity prediction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factor_id", nullable = false)
    private PredictionFactorsCatalogEntity factor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FactorImpact impact;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal weight;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
