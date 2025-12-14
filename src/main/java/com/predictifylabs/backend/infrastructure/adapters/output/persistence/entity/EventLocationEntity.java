package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.LocationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event location entity matching PostgreSQL event_locations table
 */
@Entity
@Table(name = "event_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private EventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    // Ubicación física
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(length = 200)
    private String venue;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    // Ubicación virtual
    @Column(name = "virtual_link", columnDefinition = "TEXT")
    private String virtualLink;

    @Column(name = "virtual_platform", length = 100)
    private String virtualPlatform;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
