package com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository;

import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Query("SELECT t FROM RefreshTokenEntity t WHERE t.user.id = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now")
    List<RefreshTokenEntity> findAllValidTokenByUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM RefreshTokenEntity t WHERE t.expiresAt < :now")
    List<RefreshTokenEntity> findExpiredTokens(@Param("now") OffsetDateTime now);

    void deleteByUser_Id(UUID userId);
}
