package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserEntity> findByRole(Role role);

    List<UserEntity> findByIsActiveTrue();

    @Query("SELECT u FROM UserEntity u WHERE u.role = :role AND u.isActive = true")
    List<UserEntity> findActiveByRole(@Param("role") Role role);
}
