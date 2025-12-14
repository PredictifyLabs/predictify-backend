package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.ProtectedRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProtectedRouteRepository extends JpaRepository<ProtectedRouteEntity, UUID> {

    List<ProtectedRouteEntity> findByIsActiveTrue();
}
