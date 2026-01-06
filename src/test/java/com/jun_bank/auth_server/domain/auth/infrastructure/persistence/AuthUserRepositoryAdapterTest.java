package com.jun_bank.auth_server.domain.auth.infrastructure.persistence;

import com.jun_bank.auth_server.domain.auth.application.port.out.AuthUserRepository;
import com.jun_bank.auth_server.domain.auth.application.port.out.AuthUserRepository.LoginAttemptResult;
import com.jun_bank.auth_server.domain.auth.application.port.out.AuthUserRepository.LoginAttemptStatus;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUser;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;
import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Email;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Password;
import com.jun_bank.auth_server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("AuthUserRepositoryAdapter 통합 테스트")
class AuthUserRepositoryAdapterTest extends IntegrationTestSupport {

    @Autowired
    private AuthUserRepository authUserRepository;

    private AuthUser testUser;

    @BeforeEach
    void setUp() {
        testUser = AuthUser.createBuilder()
                .userId("USR-a1b2c3d4")
                .email(Email.of("test@example.com"))
                .password(Password.of("Test1234!"))
                .role(UserRole.USER)
                .build();
    }

    // ========================================
    // 저장 테스트
    // ========================================

    @Nested
    @DisplayName("저장")
    class SaveTest {

        @Test
        @DisplayName("신규 사용자 저장")
        void save_NewUser() {
            AuthUser saved = authUserRepository.save(testUser);

            assertThat(saved.getAuthUserId()).isNotNull();
            assertThat(saved.getAuthUserId().value()).startsWith("AUT-");
            assertThat(saved.getEmail().value()).isEqualTo("test@example.com");
            assertThat(saved.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
        }

        @Test
        @DisplayName("기존 사용자 수정")
        void save_UpdateUser() {
            AuthUser saved = authUserRepository.save(testUser);
            saved.changeRole(UserRole.ADMIN);

            AuthUser updated = authUserRepository.save(saved);

            assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    // ========================================
    // 조회 테스트
    // ========================================

    @Nested
    @DisplayName("조회")
    class FindTest {

        @Test
        @DisplayName("ID로 조회")
        void findById() {
            AuthUser saved = authUserRepository.save(testUser);

            Optional<AuthUser> found = authUserRepository.findById(saved.getAuthUserId().value());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail().value()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("이메일로 조회")
        void findByEmail() {
            authUserRepository.save(testUser);

            Optional<AuthUser> found = authUserRepository.findByEmail("test@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo("USR-a1b2c3d4");
        }

        @Test
        @DisplayName("userId로 조회")
        void findByUserId() {
            authUserRepository.save(testUser);

            Optional<AuthUser> found = authUserRepository.findByUserId("USR-a1b2c3d4");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail().value()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회")
        void findById_NotFound() {
            Optional<AuthUser> found = authUserRepository.findById("AUT-a1b2c3d4");

            assertThat(found).isEmpty();
        }
    }

    // ========================================
    // 존재 여부 테스트
    // ========================================

    @Nested
    @DisplayName("존재 여부")
    class ExistsTest {

        @Test
        @DisplayName("이메일 존재 여부")
        void existsByEmail() {
            authUserRepository.save(testUser);

            assertThat(authUserRepository.existsByEmail("test@example.com")).isTrue();
            assertThat(authUserRepository.existsByEmail("notfound@example.com")).isFalse();
        }

        @Test
        @DisplayName("userId 존재 여부")
        void existsByUserId() {
            authUserRepository.save(testUser);

            assertThat(authUserRepository.existsByUserId("USR-a1b2c3d4")).isTrue();
            assertThat(authUserRepository.existsByUserId("USR-e5f6a7b8")).isFalse();
        }
    }

    // ========================================
    // 로그인 시도 관리 테스트 (Redis)
    // ========================================

    @Nested
    @DisplayName("로그인 시도 관리 (Redis)")
    class LoginAttemptTest {

        @Test
        @DisplayName("로그인 실패 기록")
        void recordLoginFailure() {
            LoginAttemptResult result = authUserRepository.recordLoginFailure("login-test@example.com");

            assertThat(result.status()).isEqualTo(LoginAttemptStatus.OK);
            assertThat(result.attempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("5회 실패 시 잠금")
        void recordLoginFailure_FiveTimes_Locked() {
            String email = "lock-test@example.com";

            LoginAttemptResult result = null;
            for (int i = 0; i < 5; i++) {
                result = authUserRepository.recordLoginFailure(email);
            }

            assertThat(result.status()).isEqualTo(LoginAttemptStatus.LOCKED);
            assertThat(result.isLocked()).isTrue();
            assertThat(result.remainingSeconds()).isGreaterThan(0);
        }

        @Test
        @DisplayName("로그인 성공 시 카운터 초기화")
        void recordLoginSuccess() {
            String email = "success-test@example.com";
            authUserRepository.recordLoginFailure(email);
            authUserRepository.recordLoginFailure(email);

            authUserRepository.recordLoginSuccess(email);

            assertThat(authUserRepository.getFailedAttempts(email)).isZero();
            assertThat(authUserRepository.isAccountLocked(email)).isFalse();
        }

        @Test
        @DisplayName("잠금 상태 확인")
        void isAccountLocked() {
            String email = "locked-check@example.com";

            for (int i = 0; i < 5; i++) {
                authUserRepository.recordLoginFailure(email);
            }

            assertThat(authUserRepository.isAccountLocked(email)).isTrue();
            assertThat(authUserRepository.getRemainingLockSeconds(email)).isGreaterThan(0);
        }

        @Test
        @DisplayName("이미 잠긴 계정에 실패 기록 시 ALREADY_LOCKED")
        void recordLoginFailure_AlreadyLocked() {
            String email = "already-locked@example.com";

            for (int i = 0; i < 5; i++) {
                authUserRepository.recordLoginFailure(email);
            }

            LoginAttemptResult result = authUserRepository.recordLoginFailure(email);

            assertThat(result.status()).isEqualTo(LoginAttemptStatus.ALREADY_LOCKED);
        }
    }

    // ========================================
    // Soft Delete 테스트
    // ========================================

    @Nested
    @DisplayName("Soft Delete")
    class SoftDeleteTest {

        @Test
        @DisplayName("사용자 삭제")
        void deleteByUserId() {
            authUserRepository.save(testUser);

            authUserRepository.deleteByUserId("USR-a1b2c3d4", "admin");

            Optional<AuthUser> found = authUserRepository.findByUserId("USR-a1b2c3d4");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("삭제된 사용자 이메일 조회 불가")
        void findByEmail_DeletedUser() {
            authUserRepository.save(testUser);
            authUserRepository.deleteByUserId("USR-a1b2c3d4", "admin");

            Optional<AuthUser> found = authUserRepository.findByEmail("test@example.com");

            assertThat(found).isEmpty();
        }
    }
}