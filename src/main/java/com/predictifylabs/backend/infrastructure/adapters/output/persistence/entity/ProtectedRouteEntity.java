package com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity;

import com.predictifylabs.backend.domain.model.Role;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Protected routes entity matching PostgreSQL protected_routes table
 * Used for frontend route authorization configuration
 */
@Entity
@Table(name = "protected_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtectedRouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String path;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Type(StringArrayType.class)
    @Column(name = "required_permissions", columnDefinition = "text[]")
    private String[] requiredPermissions;

    @Column(name = "require_all_permissions", nullable = false)
    @Builder.Default
    private Boolean requireAllPermissions = false; // TRUE = AND, FALSE = OR

    @Type(StringArrayType.class)
    @Column(name = "allowed_roles", columnDefinition = "text[]")
    private String[] allowedRoles;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "redirect_unauthorized", length = 255)
    @Builder.Default
    private String redirectUnauthorized = "/auth/login";

    @Column(name = "redirect_forbidden", length = 255)
    @Builder.Default
    private String redirectForbidden = "/403";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Short sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_route_id")
    private ProtectedRouteEntity parentRoute;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
