package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.PredictionLevel;
import com.predictifylabs.backend.domain.model.PredictionTrend;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Event predictions entity matching PostgreSQL event_predictions table
 */
@Entity
@Table(name = "event_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    // Métricas de predicción
    @Column(nullable = false)
    private Short probability; // 0-100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionLevel level;

    @Column(nullable = false)
    private Short confidence; // 0-100

    // Estimaciones de asistencia
    @Column(name = "estimated_min", nullable = false)
    private Integer estimatedMin;

    @Column(name = "estimated_max", nullable = false)
    private Integer estimatedMax;

    @Column(name = "estimated_expected", nullable = false)
    private Integer estimatedExpected;

    // Tendencia
    @Enumerated(EnumType.STRING)
    private PredictionTrend trend;

    @Column(name = "trend_change", precision = 5, scale = 2)
    private java.math.BigDecimal trendChange;

    // Timestamps
    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<EventPredictionFactorEntity> factors = new HashSet<>();
}
