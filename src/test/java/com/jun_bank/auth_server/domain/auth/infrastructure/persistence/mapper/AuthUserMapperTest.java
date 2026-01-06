package com.jun_bank.auth_server.domain.auth.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.auth.domain.model.AuthUser;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;
import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.AuthUserId;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Email;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Password;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.entity.AuthUserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthUserMapper 테스트")
class AuthUserMapperTest {

    private final AuthUserMapper mapper = new AuthUserMapper();

    @Test
    @DisplayName("toEntity - 신규 생성 시 ID 자동 생성")
    void toEntity_NewUser_GeneratesId() {
        AuthUser domain = AuthUser.createBuilder()
                .userId("USR-a1b2c3d4")
                .email(Email.of("test@example.com"))
                .password(Password.of("encoded"))
                .build();

        AuthUserEntity entity = mapper.toEntity(domain);

        assertThat(entity.getAuthUserId()).startsWith("AUT-");
        assertThat(entity.getUserId()).isEqualTo("USR-a1b2c3d4");
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("toEntity - 기존 사용자는 ID 유지")
    void toEntity_ExistingUser_KeepsId() {
        AuthUser domain = AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-e5f6a7b8"))
                .userId("USR-a1b2c3d4")
                .email(Email.of("test@example.com"))
                .password(Password.of("encoded"))
                .role(UserRole.USER)
                .status(AuthUserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .build();

        AuthUserEntity entity = mapper.toEntity(domain);

        assertThat(entity.getAuthUserId()).isEqualTo("AUT-e5f6a7b8");
    }

    @Test
    @DisplayName("toDomain - Entity → Domain 변환")
    void toDomain() {
        AuthUserEntity entity = AuthUserEntity.of(
                "AUT-a1b2c3d4",
                "USR-a1b2c3d4",
                "test@example.com",
                "encoded",
                UserRole.ADMIN,
                AuthUserStatus.LOCKED,
                3,
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now()
        );

        AuthUser domain = mapper.toDomain(entity);

        assertThat(domain.getAuthUserId().value()).isEqualTo("AUT-a1b2c3d4");
        assertThat(domain.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(domain.getStatus()).isEqualTo(AuthUserStatus.LOCKED);
        assertThat(domain.getFailedLoginAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("updateEntity - 변경 사항 반영")
    void updateEntity() {
        AuthUserEntity entity = AuthUserEntity.of(
                "AUT-a1b2c3d4", "USR-a1b2c3d4", "test@example.com",
                "oldPassword", UserRole.USER, AuthUserStatus.ACTIVE, 0, null, null
        );

        AuthUser domain = AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-a1b2c3d4"))
                .userId("USR-a1b2c3d4")
                .email(Email.of("test@example.com"))
                .password(Password.of("newPassword"))
                .role(UserRole.ADMIN)
                .status(AuthUserStatus.LOCKED)
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isDeleted(false)
                .build();

        mapper.updateEntity(entity, domain);

        assertThat(entity.getPassword()).isEqualTo("newPassword");
        assertThat(entity.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(entity.getStatus()).isEqualTo(AuthUserStatus.LOCKED);
    }

    @Test
    @DisplayName("양방향 변환 일관성")
    void roundTrip() {
        AuthUser original = AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-c9d0e1f2"))
                .userId("USR-a1b2c3d4")
                .email(Email.of("test@example.com"))
                .password(Password.of("encoded"))
                .role(UserRole.USER)
                .status(AuthUserStatus.ACTIVE)
                .failedLoginAttempts(2)
                .isDeleted(false)
                .build();

        AuthUserEntity entity = mapper.toEntity(original);
        AuthUser restored = mapper.toDomain(entity);

        assertThat(restored.getAuthUserId().value()).isEqualTo(original.getAuthUserId().value());
        assertThat(restored.getEmail().value()).isEqualTo(original.getEmail().value());
        assertThat(restored.getFailedLoginAttempts()).isEqualTo(original.getFailedLoginAttempts());
    }
}