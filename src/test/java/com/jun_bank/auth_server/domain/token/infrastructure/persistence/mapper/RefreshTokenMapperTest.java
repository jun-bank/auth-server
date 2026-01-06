package com.jun_bank.auth_server.domain.token.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;
import com.jun_bank.auth_server.domain.token.domain.model.vo.RefreshTokenId;
import com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity.RefreshTokenEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RefreshTokenMapper 테스트")
class RefreshTokenMapperTest {

    private final RefreshTokenMapper mapper = new RefreshTokenMapper();

    @Test
    @DisplayName("toEntity - 신규 토큰, ID 자동 생성")
    void toEntity_NewToken_GeneratesId() {
        RefreshToken domain = RefreshToken.createBuilder()
                .userId("USR-a1b2c3d4")
                .token("jwt.token.value")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .deviceInfo("Chrome/120.0")
                .ipAddress("192.168.1.100")
                .build();

        RefreshTokenEntity entity = mapper.toEntity(domain);

        assertThat(entity.getRefreshTokenId()).startsWith("RTK-");
        assertThat(entity.getUserId()).isEqualTo("USR-a1b2c3d4");
        assertThat(entity.getToken()).isEqualTo("jwt.token.value");
        assertThat(entity.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("toEntity - 기존 토큰, ID 유지")
    void toEntity_ExistingToken_KeepsId() {
        RefreshToken domain = RefreshToken.restoreBuilder()
                .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                .userId("USR-a1b2c3d4")
                .token("existing.jwt.token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .deviceInfo("Safari/17.0")
                .ipAddress("10.0.0.1")
                .build();

        RefreshTokenEntity entity = mapper.toEntity(domain);

        assertThat(entity.getRefreshTokenId()).isEqualTo("RTK-a1b2c3d4");
    }

    @Test
    @DisplayName("toDomain - Entity → Domain 변환")
    void toDomain() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        RefreshTokenEntity entity = RefreshTokenEntity.of(
                "RTK-e5f6a7b8",
                "USR-a1b2c3d4",
                "domain.jwt.token",
                expiresAt,
                "Chrome/120.0",
                "192.168.1.100"
        );

        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.getRefreshTokenId().value()).isEqualTo("RTK-e5f6a7b8");
        assertThat(domain.getUserId()).isEqualTo("USR-a1b2c3d4");
        assertThat(domain.getToken()).isEqualTo("domain.jwt.token");
        assertThat(domain.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(domain.isRevoked()).isFalse();
        assertThat(domain.isNew()).isFalse();
    }

    @Test
    @DisplayName("toDomain - 폐기된 토큰 변환")
    void toDomain_RevokedToken() {
        RefreshTokenEntity entity = RefreshTokenEntity.of(
                "RTK-c9d0e1f2",
                "USR-a1b2c3d4",
                "revoked.jwt.token",
                LocalDateTime.now().plusDays(7),
                "Firefox/120.0",
                "10.0.0.1"
        );
        entity.revoke();

        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.isRevoked()).isTrue();
        assertThat(domain.isValid()).isFalse();
    }

    @Test
    @DisplayName("revokeEntity - Entity 폐기")
    void revokeEntity() {
        RefreshTokenEntity entity = RefreshTokenEntity.of(
                "RTK-d1e2f3a4",
                "USR-a1b2c3d4",
                "to.revoke.token",
                LocalDateTime.now().plusDays(7),
                "Chrome/120.0",
                "192.168.1.1"
        );
        assertThat(entity.isRevoked()).isFalse();

        mapper.revokeEntity(entity);

        assertThat(entity.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("양방향 변환 일관성")
    void roundTrip() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        RefreshToken original = RefreshToken.restoreBuilder()
                .refreshTokenId(RefreshTokenId.of("RTK-f5a6b7c8"))
                .userId("USR-d9e0f1a2")
                .token("roundtrip.jwt.token")
                .expiresAt(expiresAt)
                .isRevoked(false)
                .deviceInfo("Edge/120.0")
                .ipAddress("172.16.0.1")
                .build();

        RefreshTokenEntity entity = mapper.toEntity(original);
        RefreshToken restored = mapper.toDomain(entity);

        assertThat(restored.getRefreshTokenId().value()).isEqualTo(original.getRefreshTokenId().value());
        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getToken()).isEqualTo(original.getToken());
        assertThat(restored.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(restored.isRevoked()).isEqualTo(original.isRevoked());
    }
}