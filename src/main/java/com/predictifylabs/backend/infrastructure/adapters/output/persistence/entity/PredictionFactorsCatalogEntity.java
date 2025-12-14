package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.FactorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Prediction factors catalog entity matching PostgreSQL
 * prediction_factors_catalog table
 */
@Entity
@Table(name = "prediction_factors_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionFactorsCatalogEntity {

    @Id
    @Column(length = 50)
    private String id; // e.g., 'high_engagement', 'trending_topic'

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FactorType type;

    @Column(name = "default_weight", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal defaultWeight = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
