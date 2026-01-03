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

    List<RefreshTokenEntity>

}
