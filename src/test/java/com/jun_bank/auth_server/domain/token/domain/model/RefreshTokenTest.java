package com.jun_bank.auth_server.domain.token.domain.model;

import com.jun_bank.auth_server.domain.token.domain.exception.TokenException;
import com.jun_bank.auth_server.domain.token.domain.model.vo.RefreshTokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RefreshToken 도메인 테스트")
class RefreshTokenTest {

    // ========================================
    // 생성 테스트
    // ========================================

    @Nested
    @DisplayName("생성")
    class CreateTest {

        @Test
        @DisplayName("신규 토큰 생성")
        void create_NewToken() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("jwt.token.value")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.100")
                    .build();

            assertThat(token.isNew()).isTrue();
            assertThat(token.getUserId()).isEqualTo("USR-e5f6a7b8");
            assertThat(token.getToken()).isEqualTo("jwt.token.value");
            assertThat(token.isRevoked()).isFalse();
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("토큰 없이 생성 시 예외")
        void create_WithoutToken_ThrowsException() {
            assertThatThrownBy(() -> RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build())
                    .isInstanceOf(TokenException.class);
        }

        @Test
        @DisplayName("빈 토큰으로 생성 시 예외")
        void create_EmptyToken_ThrowsException() {
            assertThatThrownBy(() -> RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build())
                    .isInstanceOf(TokenException.class);
        }
    }

    // ========================================
    // 만료 테스트
    // ========================================

    @Nested
    @DisplayName("만료")
    class ExpirationTest {

        @Test
        @DisplayName("만료되지 않은 토큰")
        void isExpired_NotExpired() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("valid.jwt.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            assertThat(token.isExpired()).isFalse();
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰")
        void isExpired_Expired() {
            RefreshToken token = RefreshToken.restoreBuilder()
                    .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .token("expired.jwt.token")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .isRevoked(false)
                    .build();

            assertThat(token.isExpired()).isTrue();
            assertThat(token.isValid()).isFalse();
        }
    }

    // ========================================
    // 폐기 테스트
    // ========================================

    @Nested
    @DisplayName("폐기")
    class RevokeTest {

        @Test
        @DisplayName("토큰 폐기")
        void revoke_Success() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("to.revoke.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            token.revoke();

            assertThat(token.isRevoked()).isTrue();
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("이미 폐기된 토큰 재폐기 (멱등성)")
        void revoke_AlreadyRevoked() {
            RefreshToken token = RefreshToken.restoreBuilder()
                    .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .token("already.revoked")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .isRevoked(true)
                    .build();

            token.revoke();

            assertThat(token.isRevoked()).isTrue();
        }
    }

    // ========================================
    // 유효성 검증 테스트
    // ========================================

    @Nested
    @DisplayName("유효성 검증")
    class ValidateTest {

        @Test
        @DisplayName("유효한 토큰 검증 통과")
        void validateForUse_Valid() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("valid.jwt.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            assertThatCode(token::validateForUse).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("폐기된 토큰 검증 시 예외")
        void validateForUse_Revoked_ThrowsException() {
            RefreshToken token = RefreshToken.restoreBuilder()
                    .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .token("revoked.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .isRevoked(true)
                    .build();

            assertThatThrownBy(token::validateForUse)
                    .isInstanceOf(TokenException.class);
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 예외")
        void validateForUse_Expired_ThrowsException() {
            RefreshToken token = RefreshToken.restoreBuilder()
                    .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .token("expired.token")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .isRevoked(false)
                    .build();

            assertThatThrownBy(token::validateForUse)
                    .isInstanceOf(TokenException.class);
        }
    }

    // ========================================
    // 컨텍스트 매칭 테스트
    // ========================================

    @Nested
    @DisplayName("컨텍스트 매칭")
    class MatchesContextTest {

        @Test
        @DisplayName("동일한 디바이스/IP 매칭")
        void matchesContext_Same() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("context.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.100")
                    .build();

            assertThat(token.matchesContext("Chrome/120.0", "192.168.1.100")).isTrue();
        }

        @Test
        @DisplayName("다른 디바이스 불일치")
        void matchesContext_DifferentDevice() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("context.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.100")
                    .build();

            assertThat(token.matchesContext("Firefox/120.0", "192.168.1.100")).isFalse();
        }

        @Test
        @DisplayName("다른 IP 불일치")
        void matchesContext_DifferentIp() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("context.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.100")
                    .build();

            assertThat(token.matchesContext("Chrome/120.0", "10.0.0.1")).isFalse();
        }

        @Test
        @DisplayName("토큰에 deviceInfo가 null이면 검증 스킵")
        void matchesContext_NullDeviceInfo() {
            RefreshToken token = RefreshToken.createBuilder()
                    .userId("USR-e5f6a7b8")
                    .token("context.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo(null)
                    .ipAddress("192.168.1.100")
                    .build();

            assertThat(token.matchesContext("AnyDevice", "192.168.1.100")).isTrue();
        }
    }

    // ========================================
    // RestoreBuilder 테스트
    // ========================================

    @Nested
    @DisplayName("RestoreBuilder")
    class RestoreBuilderTest {

        @Test
        @DisplayName("DB 복원용 빌더로 생성")
        void restoreBuilder() {
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            LocalDateTime createdAt = LocalDateTime.now().minusHours(1);

            RefreshToken token = RefreshToken.restoreBuilder()
                    .refreshTokenId(RefreshTokenId.of("RTK-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .token("restored.jwt.token")
                    .expiresAt(expiresAt)
                    .isRevoked(false)
                    .deviceInfo("Safari/17.0")
                    .ipAddress("10.0.0.1")
                    .createdAt(createdAt)
                    .build();

            assertThat(token.isNew()).isFalse();
            assertThat(token.getRefreshTokenId().value()).isEqualTo("RTK-a1b2c3d4");
            assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        }
    }
}