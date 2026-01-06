package com.jun_bank.auth_server.domain.auth.domain.model;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.AuthUserId;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Email;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthUser 도메인 테스트")
class AuthUserTest {

    // ========================================
    // 생성 테스트
    // ========================================

    @Nested
    @DisplayName("생성")
    class CreateTest {

        @Test
        @DisplayName("신규 생성 시 기본값이 설정된다")
        void create_WithDefaults() {
            AuthUser authUser = AuthUser.createBuilder()
                    .userId("USR-12345678")
                    .email(Email.of("test@example.com"))
                    .password(Password.of("encodedPassword"))
                    .build();

            assertThat(authUser.isNew()).isTrue();
            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
            assertThat(authUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(authUser.getFailedLoginAttempts()).isZero();
            assertThat(authUser.canLogin()).isTrue();
        }

        @Test
        @DisplayName("이메일 없이 생성 시 예외 발생")
        void create_WithoutEmail_ThrowsException() {
            assertThatThrownBy(() -> AuthUser.createBuilder()
                    .userId("USR-12345678")
                    .password(Password.of("encodedPassword"))
                    .build())
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("비밀번호 없이 생성 시 예외 발생")
        void create_WithoutPassword_ThrowsException() {
            assertThatThrownBy(() -> AuthUser.createBuilder()
                    .userId("USR-12345678")
                    .email(Email.of("test@example.com"))
                    .build())
                    .isInstanceOf(AuthException.class);
        }
    }

    // ========================================
    // 로그인 실패 & 잠금 테스트
    // ========================================

    @Nested
    @DisplayName("로그인 실패 처리")
    class LoginFailureTest {

        @Test
        @DisplayName("5회 실패 시 계정이 잠긴다")
        void recordLoginFailure_FiveTimes_AccountLocked() {
            AuthUser authUser = createActiveUser();

            for (int i = 0; i < 5; i++) {
                authUser.recordLoginFailure();
            }

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.LOCKED);
            assertThat(authUser.isLocked()).isTrue();
            assertThat(authUser.canLogin()).isFalse();
            assertThat(authUser.getLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("4회 실패까지는 잠기지 않는다")
        void recordLoginFailure_FourTimes_NotLocked() {
            AuthUser authUser = createActiveUser();

            for (int i = 0; i < 4; i++) {
                authUser.recordLoginFailure();
            }

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
            assertThat(authUser.isLocked()).isFalse();
            assertThat(authUser.getFailedLoginAttempts()).isEqualTo(4);
        }

        @Test
        @DisplayName("이미 잠긴 계정에 실패 기록 시 예외 발생")
        void recordLoginFailure_AlreadyLocked_ThrowsException() {
            AuthUser authUser = createLockedUser();

            assertThatThrownBy(authUser::recordLoginFailure)
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("비활성화 계정에 실패 기록 시 예외 발생")
        void recordLoginFailure_Disabled_ThrowsException() {
            AuthUser authUser = createDisabledUser();

            assertThatThrownBy(authUser::recordLoginFailure)
                    .isInstanceOf(AuthException.class);
        }
    }

    // ========================================
    // 로그인 성공 테스트
    // ========================================

    @Nested
    @DisplayName("로그인 성공 처리")
    class LoginSuccessTest {

        @Test
        @DisplayName("성공 시 실패 횟수가 초기화된다")
        void recordLoginSuccess_ResetsFailedAttempts() {
            AuthUser authUser = createActiveUser();
            authUser.recordLoginFailure();
            authUser.recordLoginFailure();

            authUser.recordLoginSuccess();

            assertThat(authUser.getFailedLoginAttempts()).isZero();
            assertThat(authUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("잠금 만료 후 로그인 성공 시 ACTIVE로 전환")
        void recordLoginSuccess_AfterLockExpired_BecomesActive() {
            AuthUser authUser = createLockExpiredUser();

            authUser.recordLoginSuccess();

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
            assertThat(authUser.getLockedUntil()).isNull();
        }
    }

    // ========================================
    // 잠금 해제 테스트
    // ========================================

    @Nested
    @DisplayName("잠금 해제")
    class UnlockTest {

        @Test
        @DisplayName("수동 잠금 해제")
        void unlock_LockedAccount() {
            AuthUser authUser = createLockedUser();

            authUser.unlock();

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
            assertThat(authUser.isLocked()).isFalse();
            assertThat(authUser.getFailedLoginAttempts()).isZero();
        }

        @Test
        @DisplayName("잠금 시간 만료 시 canLogin() = true")
        void canLogin_LockExpired_ReturnsTrue() {
            AuthUser authUser = createLockExpiredUser();

            assertThat(authUser.canLogin()).isTrue();
        }
    }

    // ========================================
    // 계정 상태 변경 테스트
    // ========================================

    @Nested
    @DisplayName("계정 상태 변경")
    class StatusChangeTest {

        @Test
        @DisplayName("계정 비활성화")
        void disable_Account() {
            AuthUser authUser = createActiveUser();

            authUser.disable();

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.DISABLED);
            assertThat(authUser.canLogin()).isFalse();
        }

        @Test
        @DisplayName("계정 활성화")
        void enable_DisabledAccount() {
            AuthUser authUser = createDisabledUser();

            authUser.enable();

            assertThat(authUser.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
            assertThat(authUser.canLogin()).isTrue();
        }

        @Test
        @DisplayName("비밀번호 변경")
        void changePassword() {
            AuthUser authUser = createActiveUser();
            Password newPassword = Password.of("newEncodedPassword");

            authUser.changePassword(newPassword);

            assertThat(authUser.getPassword().encodedValue()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("비활성화 상태에서 비밀번호 변경 시 예외")
        void changePassword_Disabled_ThrowsException() {
            AuthUser authUser = createDisabledUser();

            assertThatThrownBy(() -> authUser.changePassword(Password.of("new")))
                    .isInstanceOf(AuthException.class);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private AuthUser createActiveUser() {
        return AuthUser.createBuilder()
                .userId("USR-12345678")
                .email(Email.of("test@example.com"))
                .password(Password.of("encodedPassword"))
                .build();
    }

    private AuthUser createLockedUser() {
        return AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-12345678"))
                .userId("USR-12345678")
                .email(Email.of("locked@example.com"))
                .password(Password.of("encodedPassword"))
                .role(UserRole.USER)
                .status(AuthUserStatus.LOCKED)
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isDeleted(false)
                .build();
    }

    private AuthUser createLockExpiredUser() {
        return AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-12345678"))
                .userId("USR-12345678")
                .email(Email.of("expired@example.com"))
                .password(Password.of("encodedPassword"))
                .role(UserRole.USER)
                .status(AuthUserStatus.LOCKED)
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().minusMinutes(1)) // 이미 만료
                .isDeleted(false)
                .build();
    }

    private AuthUser createDisabledUser() {
        return AuthUser.restoreBuilder()
                .authUserId(AuthUserId.of("AUT-12345678"))
                .userId("USR-12345678")
                .email(Email.of("disabled@example.com"))
                .password(Password.of("encodedPassword"))
                .role(UserRole.USER)
                .status(AuthUserStatus.DISABLED)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .build();
    }
}