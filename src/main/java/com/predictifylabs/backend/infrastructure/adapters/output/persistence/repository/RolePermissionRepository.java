package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    List<RolePermissionEntity> findByRole(Role role);

    @Query("SELECT rp.permission.id FROM RolePermissionEntity rp WHERE rp.role = :role")
    List<String> findPermissionIdsByRole(@Param("role") Role role);
}
