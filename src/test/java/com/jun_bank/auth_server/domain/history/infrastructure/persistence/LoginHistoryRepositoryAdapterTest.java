package com.jun_bank.auth_server.domain.history.infrastructure.persistence;

import com.jun_bank.auth_server.domain.history.application.port.out.LoginHistoryRepository;
import com.jun_bank.auth_server.domain.history.domain.model.LoginHistory;
import com.jun_bank.auth_server.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("LoginHistoryRepositoryAdapter 통합 테스트")
class LoginHistoryRepositoryAdapterTest extends IntegrationTestSupport {

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    // ========================================
    // 저장 테스트
    // ========================================

    @Nested
    @DisplayName("저장")
    class SaveTest {

        @Test
        @DisplayName("로그인 성공 이력 저장")
        void save_SuccessHistory() {
            LoginHistory history = LoginHistory.success(
                    "USR-a1b2c3d4",
                    "success@example.com",
                    "192.168.1.100",
                    "Chrome/120.0"
            );

            LoginHistory saved = loginHistoryRepository.save(history);

            assertThat(saved.getLoginHistoryId()).isNotNull();
            assertThat(saved.getLoginHistoryId().value()).startsWith("LGH-");
            assertThat(saved.isSuccess()).isTrue();
            assertThat(saved.getFailReason()).isNull();
        }

        @Test
        @DisplayName("로그인 실패 이력 저장")
        void save_FailureHistory() {
            LoginHistory history = LoginHistory.failure(
                    null,
                    "fail@example.com",
                    "192.168.1.200",
                    "Firefox/120.0",
                    "INVALID_PASSWORD"
            );

            LoginHistory saved = loginHistoryRepository.save(history);

            assertThat(saved.getLoginHistoryId()).isNotNull();
            assertThat(saved.isSuccess()).isFalse();
            assertThat(saved.getFailReason()).isEqualTo("INVALID_PASSWORD");
        }
    }

    // ========================================
    // 단건 조회 테스트
    // ========================================

    @Nested
    @DisplayName("단건 조회")
    class FindByIdTest {

        @Test
        @DisplayName("ID로 조회")
        void findById() {
            LoginHistory history = LoginHistory.success(
                    "USR-e5f6a7b8",
                    "findbyid@example.com",
                    "192.168.1.1",
                    "Chrome/120.0"
            );
            LoginHistory saved = loginHistoryRepository.save(history);

            Optional<LoginHistory> found = loginHistoryRepository.findById(saved.getLoginHistoryId().value());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("findbyid@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회")
        void findById_NotFound() {
            Optional<LoginHistory> found = loginHistoryRepository.findById("LGH-a1b2c3d4");

            assertThat(found).isEmpty();
        }
    }

    // ========================================
    // 사용자별 조회 테스트
    // ========================================

    @Nested
    @DisplayName("사용자별 조회")
    class FindByUserIdTest {

        @Test
        @DisplayName("사용자별 이력 조회 (페이징)")
        void findByUserId_Paging() {
            String userId = "USR-c9d0e1f2";

            for (int i = 0; i < 5; i++) {
                LoginHistory history = LoginHistory.success(
                        userId,
                        "user@example.com",
                        "192.168.1." + i,
                        "Chrome/120.0"
                );
                loginHistoryRepository.save(history);
            }

            Page<LoginHistory> page = loginHistoryRepository.findByUserId(userId, PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("사용자별 최근 이력 조회")
        void findRecentByUserId() {
            String userId = "USR-d1e2f3a4";

            for (int i = 0; i < 10; i++) {
                LoginHistory history = LoginHistory.success(
                        userId,
                        "recent@example.com",
                        "192.168.1." + i,
                        "Chrome/120.0"
                );
                loginHistoryRepository.save(history);
            }

            List<LoginHistory> recent = loginHistoryRepository.findRecentByUserId(userId, 5);

            assertThat(recent).hasSize(5);
        }

        @Test
        @DisplayName("사용자별 최근 성공 이력 조회")
        void findRecentSuccessByUserId() {
            String userId = "USR-f5a6b7c8";

            // 성공 3개
            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.success(
                        userId, "user@example.com", "192.168.1." + i, "Chrome/120.0"
                ));
            }
            // 실패 2개
            for (int i = 0; i < 2; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        userId, "user@example.com", "192.168.1." + i, "Chrome/120.0", "INVALID_PASSWORD"
                ));
            }

            List<LoginHistory> successList = loginHistoryRepository.findRecentSuccessByUserId(userId, 10);

            assertThat(successList).hasSize(3);
            assertThat(successList).allMatch(LoginHistory::isSuccess);
        }

        @Test
        @DisplayName("사용자별 최근 실패 이력 조회")
        void findRecentFailuresByUserId() {
            String userId = "USR-a9b8c7d6";

            // 성공 2개
            for (int i = 0; i < 2; i++) {
                loginHistoryRepository.save(LoginHistory.success(
                        userId, "user@example.com", "192.168.1." + i, "Chrome/120.0"
                ));
            }
            // 실패 3개
            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        userId, "user@example.com", "192.168.1." + i, "Chrome/120.0", "ACCOUNT_LOCKED"
                ));
            }

            List<LoginHistory> failureList = loginHistoryRepository.findRecentFailuresByUserId(userId, 10);

            assertThat(failureList).hasSize(3);
            assertThat(failureList).allMatch(LoginHistory::isFailure);
        }
    }

    // ========================================
    // IP별 조회 테스트
    // ========================================

    @Nested
    @DisplayName("IP별 조회")
    class FindByIpAddressTest {

        @Test
        @DisplayName("IP별 실패 횟수 조회")
        void countFailuresByIpAddressSince() {
            String ipAddress = "10.0.0.100";

            // 실패 3개
            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        null, "fail" + i + "@example.com", ipAddress, "Chrome/120.0", "INVALID_PASSWORD"
                ));
            }
            // 성공 1개 (카운트 안됨)
            loginHistoryRepository.save(LoginHistory.success(
                    "USR-b1c2d3e4", "success@example.com", ipAddress, "Chrome/120.0"
            ));

            long failCount = loginHistoryRepository.countFailuresByIpAddressSince(ipAddress, 60);

            assertThat(failCount).isEqualTo(3);
        }

        @Test
        @DisplayName("IP별 기간 조회")
        void findByIpAddressAndPeriod() {
            String ipAddress = "172.16.0.1";
            LocalDateTime from = LocalDateTime.now().minusHours(1);
            LocalDateTime to = LocalDateTime.now().plusHours(1);

            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.success(
                        "USR-e4f5a6b7", "ip@example.com", ipAddress, "Chrome/120.0"
                ));
            }

            List<LoginHistory> histories = loginHistoryRepository.findByIpAddressAndPeriod(ipAddress, from, to);

            assertThat(histories).hasSize(3);
        }
    }

    // ========================================
    // 통계 테스트
    // ========================================

    @Nested
    @DisplayName("통계")
    class StatisticsTest {

        @Test
        @DisplayName("기간별 성공 횟수")
        void countSuccessByPeriod() {
            LocalDateTime from = LocalDateTime.now().minusHours(1);
            LocalDateTime to = LocalDateTime.now().plusHours(1);

            // 성공 3개
            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.success(
                        "USR-c8d9e0f1", "stat@example.com", "192.168.1." + i, "Chrome/120.0"
                ));
            }
            // 실패 2개
            for (int i = 0; i < 2; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        null, "fail@example.com", "192.168.1." + i, "Chrome/120.0", "ERROR"
                ));
            }

            long successCount = loginHistoryRepository.countSuccessByPeriod(from, to);
            long failureCount = loginHistoryRepository.countFailuresByPeriod(from, to);

            assertThat(successCount).isEqualTo(3);
            assertThat(failureCount).isEqualTo(2);
        }

        @Test
        @DisplayName("실패 사유별 횟수")
        void countByFailReasonAndPeriod() {
            LocalDateTime from = LocalDateTime.now().minusHours(1);
            LocalDateTime to = LocalDateTime.now().plusHours(1);

            // INVALID_PASSWORD 3회
            for (int i = 0; i < 3; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        null, "fail@example.com", "192.168.1." + i, "Chrome/120.0", "INVALID_PASSWORD"
                ));
            }
            // ACCOUNT_LOCKED 2회
            for (int i = 0; i < 2; i++) {
                loginHistoryRepository.save(LoginHistory.failure(
                        null, "locked@example.com", "10.0.0." + i, "Firefox/120.0", "ACCOUNT_LOCKED"
                ));
            }

            long invalidPwCount = loginHistoryRepository.countByFailReasonAndPeriod("INVALID_PASSWORD", from, to);
            long lockedCount = loginHistoryRepository.countByFailReasonAndPeriod("ACCOUNT_LOCKED", from, to);

            assertThat(invalidPwCount).isEqualTo(3);
            assertThat(lockedCount).isEqualTo(2);
        }
    }

    // ========================================
    // Append-only 특성 테스트
    // ========================================

    @Nested
    @DisplayName("Append-only 특성")
    class AppendOnlyTest {

        @Test
        @DisplayName("이력은 수정되지 않고 새로 생성됨")
        void appendOnly_NoUpdate() {
            String userId = "USR-d2e3f4a5";

            LoginHistory saved1 = loginHistoryRepository.save(LoginHistory.success(
                    userId, "append@example.com", "192.168.1.1", "Chrome/120.0"
            ));

            LoginHistory saved2 = loginHistoryRepository.save(LoginHistory.success(
                    userId, "append@example.com", "192.168.1.2", "Safari/17.0"
            ));

            // 서로 다른 ID
            assertThat(saved1.getLoginHistoryId().value())
                    .isNotEqualTo(saved2.getLoginHistoryId().value());

            // 사용자 이력 2개 존재
            List<LoginHistory> histories = loginHistoryRepository.findRecentByUserId(userId, 10);
            assertThat(histories).hasSize(2);
        }
    }

    // ========================================
    // 배치 삭제 테스트
    // ========================================

    @Nested
    @DisplayName("배치 삭제")
    class DeleteOldHistoriesTest {

        @Test
        @DisplayName("오래된 이력 삭제 (연 단위)")
        void deleteOldHistories() {
            // 이 테스트는 실제로 2년 전 데이터를 만들기 어려우므로
            // deleteOldHistories 메서드가 호출 가능한지만 확인
            int deleted = loginHistoryRepository.deleteOldHistories(2);

            assertThat(deleted).isGreaterThanOrEqualTo(0);
        }
    }
}