package com.jun_bank.auth_server.domain.history.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.history.domain.model.LoginHistory;
import com.jun_bank.auth_server.domain.history.domain.model.vo.LoginHistoryId;
import com.jun_bank.auth_server.domain.history.infrastructure.persistence.entity.LoginHistoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginHistoryMapper 테스트")
class LoginHistoryMapperTest {

    private final LoginHistoryMapper mapper = new LoginHistoryMapper();

    @Test
    @DisplayName("toEntity - 성공 이력, 신규 생성 시 ID 자동 생성")
    void toEntity_Success_NewHistory() {
        LoginHistory domain = LoginHistory.success(
                "USR-a1b2c3d4",
                "success@example.com",
                "192.168.1.100",
                "Chrome/120.0"
        );

        LoginHistoryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getLoginHistoryId()).startsWith("LGH-");
        assertThat(entity.getUserId()).isEqualTo("USR-a1b2c3d4");
        assertThat(entity.getEmail()).isEqualTo("success@example.com");
        assertThat(entity.isSuccess()).isTrue();
        assertThat(entity.getFailReason()).isNull();
    }

    @Test
    @DisplayName("toEntity - 실패 이력")
    void toEntity_Failure() {
        LoginHistory domain = LoginHistory.failure(
                null,
                "fail@example.com",
                "192.168.1.200",
                "Firefox/120.0",
                "INVALID_PASSWORD"
        );

        LoginHistoryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getLoginHistoryId()).startsWith("LGH-");
        assertThat(entity.getUserId()).isNull();
        assertThat(entity.isSuccess()).isFalse();
        assertThat(entity.getFailReason()).isEqualTo("INVALID_PASSWORD");
    }

    @Test
    @DisplayName("toEntity - 기존 이력은 ID 유지")
    void toEntity_ExistingHistory_KeepsId() {
        LoginHistory domain = LoginHistory.restoreBuilder()
                .loginHistoryId(LoginHistoryId.of("LGH-e5f6a7b8"))
                .userId("USR-a1b2c3d4")
                .email("existing@example.com")
                .loginAt(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .userAgent("Chrome/120.0")
                .success(true)
                .build();

        LoginHistoryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getLoginHistoryId()).isEqualTo("LGH-e5f6a7b8");
    }

    @Test
    @DisplayName("toDomain - 성공 이력 변환")
    void toDomain_Success() {
        LocalDateTime loginAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        LoginHistoryEntity entity = LoginHistoryEntity.success(
                "LGH-c9d0e1f2",
                "USR-a1b2c3d4",
                "domain@example.com",
                loginAt,
                "10.0.0.1",
                "Safari/17.0"
        );

        LoginHistory domain = mapper.toDomain(entity);

        assertThat(domain.getLoginHistoryId().value()).isEqualTo("LGH-c9d0e1f2");
        assertThat(domain.getUserId()).isEqualTo("USR-a1b2c3d4");
        assertThat(domain.getEmail()).isEqualTo("domain@example.com");
        assertThat(domain.getLoginAt()).isEqualTo(loginAt);
        assertThat(domain.isSuccess()).isTrue();
        assertThat(domain.isNew()).isFalse();
    }

    @Test
    @DisplayName("toDomain - 실패 이력 변환")
    void toDomain_Failure() {
        LoginHistoryEntity entity = LoginHistoryEntity.failure(
                "LGH-f1a2b3c4",
                null,
                "fail@example.com",
                LocalDateTime.now(),
                "192.168.1.1",
                "Chrome/120.0",
                "ACCOUNT_LOCKED"
        );

        LoginHistory domain = mapper.toDomain(entity);

        assertThat(domain.isFailure()).isTrue();
        assertThat(domain.getFailReason()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(domain.getUserId()).isNull();
    }

    @Test
    @DisplayName("양방향 변환 일관성 - 성공")
    void roundTrip_Success() {
        LoginHistory original = LoginHistory.restoreBuilder()
                .loginHistoryId(LoginHistoryId.of("LGH-d5e6f7a8"))
                .userId("USR-b9c0d1e2")
                .email("round@example.com")
                .loginAt(LocalDateTime.of(2025, 1, 1, 10, 30))
                .ipAddress("172.16.0.1")
                .userAgent("Edge/120.0")
                .success(true)
                .build();

        LoginHistoryEntity entity = mapper.toEntity(original);
        LoginHistory restored = mapper.toDomain(entity);

        assertThat(restored.getLoginHistoryId().value()).isEqualTo(original.getLoginHistoryId().value());
        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getEmail()).isEqualTo(original.getEmail());
        assertThat(restored.getLoginAt()).isEqualTo(original.getLoginAt());
        assertThat(restored.isSuccess()).isEqualTo(original.isSuccess());
    }

    @Test
    @DisplayName("양방향 변환 일관성 - 실패")
    void roundTrip_Failure() {
        LoginHistory original = LoginHistory.restoreBuilder()
                .loginHistoryId(LoginHistoryId.of("LGH-a9b8c7d6"))
                .userId(null)
                .email("failround@example.com")
                .loginAt(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .userAgent("Chrome/120.0")
                .success(false)
                .failReason("USER_NOT_FOUND")
                .build();

        LoginHistoryEntity entity = mapper.toEntity(original);
        LoginHistory restored = mapper.toDomain(entity);

        assertThat(restored.isFailure()).isTrue();
        assertThat(restored.getFailReason()).isEqualTo(original.getFailReason());
    }
}