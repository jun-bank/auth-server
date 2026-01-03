package com.jun_bank.auth_server.domain.token.infrastructure.persistence.jpa;

import com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.token = :token " +
            "AND r.isRevoked = false AND r.expiresAt > :now")
    Optional<RefreshTokenEntity> findValidToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );

    List<RefreshTokenEntity> findByUserId(String userId);

    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.userId = :userId " +
            "AND r.isRevoked = false AND r.expiresAt > :now " +
            "ORDER BY r.createdAt DESC")
    List<RefreshTokenEntity> findValidTokensByUserId(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(r) FROM RefreshTokenEntity r WHERE r.userId " +
            "AND r.isRevoked = false AND r.expiresAt > :now")
    long countValidTokensByUserId(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now
    );
}
