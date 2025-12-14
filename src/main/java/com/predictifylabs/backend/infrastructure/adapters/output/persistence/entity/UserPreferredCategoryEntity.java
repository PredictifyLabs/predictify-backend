package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.EventCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User preferred categories entity matching PostgreSQL
 * user_preferred_categories table
 */
@Entity
@Table(name = "user_preferred_categories", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "category" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferredCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
