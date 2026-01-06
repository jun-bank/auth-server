package com.jun_bank.auth_server.domain.history.domain.model;

import com.jun_bank.auth_server.domain.history.domain.model.vo.LoginHistoryId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginHistory 도메인 테스트")
class LoginHistoryTest {

    // ========================================
    // 팩토리 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("success 팩토리 메서드")
    class SuccessFactoryTest {

        @Test
        @DisplayName("로그인 성공 이력 생성")
        void success() {
            LoginHistory history = LoginHistory.success(
                    "USR-a1b2c3d4",
                    "test@example.com",
                    "192.168.1.100",
                    "Chrome/120.0"
            );

            assertThat(history.isNew()).isTrue();
            assertThat(history.getLoginHistoryId()).isNull();
            assertThat(history.getUserId()).isEqualTo("USR-a1b2c3d4");
            assertThat(history.getEmail()).isEqualTo("test@example.com");
            assertThat(history.getIpAddress()).isEqualTo("192.168.1.100");
            assertThat(history.getUserAgent()).isEqualTo("Chrome/120.0");
            assertThat(history.isSuccess()).isTrue();
            assertThat(history.isFailure()).isFalse();
            assertThat(history.getFailReason()).isNull();
            assertThat(history.getLoginAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("failure 팩토리 메서드")
    class FailureFactoryTest {

        @Test
        @DisplayName("로그인 실패 이력 생성 - 비밀번호 오류")
        void failure_InvalidPassword() {
            LoginHistory history = LoginHistory.failure(
                    "USR-a1b2c3d4",
                    "test@example.com",
                    "192.168.1.100",
                    "Chrome/120.0",
                    "INVALID_PASSWORD"
            );

            assertThat(history.isNew()).isTrue();
            assertThat(history.isSuccess()).isFalse();
            assertThat(history.isFailure()).isTrue();
            assertThat(history.getFailReason()).isEqualTo("INVALID_PASSWORD");
            assertThat(history.getUserId()).isEqualTo("USR-a1b2c3d4");
        }

        @Test
        @DisplayName("로그인 실패 이력 생성 - 사용자 없음")
        void failure_UserNotFound() {
            LoginHistory history = LoginHistory.failure(
                    null,  // 사용자를 찾을 수 없는 경우
                    "unknown@example.com",
                    "192.168.1.100",
                    "Chrome/120.0",
                    "USER_NOT_FOUND"
            );

            assertThat(history.getUserId()).isNull();
            assertThat(history.getEmail()).isEqualTo("unknown@example.com");
            assertThat(history.getFailReason()).isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("로그인 실패 이력 생성 - 계정 잠금")
        void failure_AccountLocked() {
            LoginHistory history = LoginHistory.failure(
                    "USR-a1b2c3d4",
                    "locked@example.com",
                    "192.168.1.100",
                    "Chrome/120.0",
                    "ACCOUNT_LOCKED"
            );

            assertThat(history.getFailReason()).isEqualTo("ACCOUNT_LOCKED");
        }
    }

    // ========================================
    // 상태 확인 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("상태 확인")
    class StatusTest {

        @Test
        @DisplayName("isNew - 신규 이력")
        void isNew_True() {
            LoginHistory history = LoginHistory.success(
                    "USR-a1b2c3d4", "test@example.com", "192.168.1.1", "Chrome"
            );

            assertThat(history.isNew()).isTrue();
        }

        @Test
        @DisplayName("isNew - 복원된 이력")
        void isNew_False() {
            LoginHistory history = LoginHistory.restoreBuilder()
                    .loginHistoryId(LoginHistoryId.of("LGH-a1b2c3d4"))
                    .userId("USR-e5f6a7b8")
                    .email("test@example.com")
                    .loginAt(LocalDateTime.now())
                    .ipAddress("192.168.1.1")
                    .userAgent("Chrome")
                    .success(true)
                    .build();

            assertThat(history.isNew()).isFalse();
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
        void restoreBuilder_Success() {
            LocalDateTime loginAt = LocalDateTime.of(2025, 1, 1, 12, 0);

            LoginHistory history = LoginHistory.restoreBuilder()
                    .loginHistoryId(LoginHistoryId.of("LGH-c9d0e1f2"))
                    .userId("USR-a1b2c3d4")
                    .email("restored@example.com")
                    .loginAt(loginAt)
                    .ipAddress("10.0.0.1")
                    .userAgent("Safari/17.0")
                    .success(true)
                    .failReason(null)
                    .build();

            assertThat(history.isNew()).isFalse();
            assertThat(history.getLoginHistoryId().value()).isEqualTo("LGH-c9d0e1f2");
            assertThat(history.getLoginAt()).isEqualTo(loginAt);
        }

        @Test
        @DisplayName("실패 이력 복원")
        void restoreBuilder_Failure() {
            LoginHistory history = LoginHistory.restoreBuilder()
                    .loginHistoryId(LoginHistoryId.of("LGH-f1e2d3c4"))
                    .userId(null)
                    .email("fail@example.com")
                    .loginAt(LocalDateTime.now())
                    .ipAddress("192.168.1.1")
                    .userAgent("Chrome/120.0")
                    .success(false)
                    .failReason("ACCOUNT_LOCKED")
                    .build();

            assertThat(history.isFailure()).isTrue();
            assertThat(history.getFailReason()).isEqualTo("ACCOUNT_LOCKED");
        }
    }

    // ========================================
    // Append-only 특성 테스트
    // ========================================

    @Nested
    @DisplayName("Append-only 특성")
    class AppendOnlyTest {

        @Test
        @DisplayName("상태 변경 메서드가 없음을 확인")
        void noMutationMethods() {
            // LoginHistory 클래스에는 상태를 변경하는 메서드가 없어야 함
            // 이 테스트는 문서화 목적
            LoginHistory history = LoginHistory.success(
                    "USR-a1b2c3d4", "test@example.com", "192.168.1.1", "Chrome"
            );

            // Getter만 존재, Setter/Update 메서드 없음
            assertThat(history.getUserId()).isNotNull();
            assertThat(history.getEmail()).isNotNull();
            assertThat(history.getLoginAt()).isNotNull();
        }
    }
}