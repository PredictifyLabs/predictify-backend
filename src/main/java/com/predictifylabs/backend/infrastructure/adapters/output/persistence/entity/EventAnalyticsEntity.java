package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event analytics entity matching PostgreSQL event_analytics table
 */
@Entity
@Table(name = "event_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    // Overview
    @Column(name = "total_views", nullable = false)
    @Builder.Default
    private Integer totalViews = 0;

    @Column(name = "total_interested", nullable = false)
    @Builder.Default
    private Integer totalInterested = 0;

    @Column(name = "total_registered", nullable = false)
    @Builder.Default
    private Integer totalRegistered = 0;

    @Column(name = "total_attended")
    private Integer totalAttended;

    @Column(name = "conversion_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "attendance_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal attendanceRate = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    // Engagement
    @Column(name = "email_open_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal emailOpenRate = BigDecimal.ZERO;

    @Column(name = "email_click_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal emailClickRate = BigDecimal.ZERO;

    @Column(name = "social_shares", nullable = false)
    @Builder.Default
    private Integer socialShares = 0;

    @Column(name = "average_time_on_page")
    private Integer averageTimeOnPage; // en segundos

    @Column(name = "bounce_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal bounceRate = BigDecimal.ZERO;

    // Precisión de predicción
    @Column(name = "predicted_attendance")
    private Integer predictedAttendance;

    @Column(name = "actual_attendance")
    private Integer actualAttendance;

    @Column(name = "prediction_accuracy", precision = 5, scale = 4)
    private BigDecimal predictionAccuracy;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
