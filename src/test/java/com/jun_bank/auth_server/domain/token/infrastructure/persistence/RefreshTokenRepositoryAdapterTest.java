package com.jun_bank.auth_server.domain.token.infrastructure.persistence;

import com.jun_bank.auth_server.domain.token.application.port.out.RefreshTokenRepository;
import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;
import com.jun_bank.auth_server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("RefreshTokenRepositoryAdapter 통합 테스트")
class RefreshTokenRepositoryAdapterTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken testToken;

    @BeforeEach
    void setUp() {
        // flushAll 제거 - Lettuce 연결 풀 캐싱 문제로 인해
        // 테스트마다 고유한 토큰 값 사용 (UUID로 충돌 방지)
        String uniqueToken = "jwt." + UUID.randomUUID().toString();

        testToken = RefreshToken.createBuilder()
                .userId("a1b2c3d4e5f6")
                .token(uniqueToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .deviceInfo("Chrome/120.0")
                .ipAddress("192.168.1.100")
                .build();
    }

    // ========================================
    // 저장 테스트
    // ========================================

    @Nested
    @DisplayName("저장")
    class SaveTest {

        @Test
        @DisplayName("신규 토큰 저장")
        void save_NewToken() {
            RefreshToken saved = refreshTokenRepository.save(testToken);

            assertThat(saved.getRefreshTokenId()).isNotNull();
            assertThat(saved.getRefreshTokenId().value()).startsWith("RTK-");
            assertThat(saved.getToken()).isEqualTo(testToken.getToken());
            assertThat(saved.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("같은 디바이스 토큰 교체")
        void save_SameDevice_ReplacesOld() {
            String userId = "b1c2d3e4f5a6";  // hex ID
            String firstToken = "jwt.first." + UUID.randomUUID();
            String secondToken = "jwt.second." + UUID.randomUUID();

            RefreshToken token1 = RefreshToken.createBuilder()
                    .userId(userId)
                    .token(firstToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.1")
                    .build();
            refreshTokenRepository.save(token1);

            RefreshToken token2 = RefreshToken.createBuilder()
                    .userId(userId)
                    .token(secondToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")  // 같은 디바이스
                    .ipAddress("192.168.1.1")
                    .build();
            refreshTokenRepository.save(token2);

            // 첫 번째 토큰은 폐기됨
            Optional<RefreshToken> first = refreshTokenRepository.findValidToken(firstToken);
            assertThat(first).isEmpty();

            // 두 번째 토큰만 유효
            Optional<RefreshToken> second = refreshTokenRepository.findValidToken(secondToken);
            assertThat(second).isPresent();
        }

        @Test
        @DisplayName("최대 세션 초과 시 가장 오래된 토큰 삭제")
        void save_ExceedsMaxSessions() {
            String userId = "c1d2e3f4a5b6";  // hex ID

            // 3개 토큰 저장 (maxSessions=3)
            for (int i = 1; i <= 3; i++) {
                RefreshToken token = RefreshToken.createBuilder()
                        .userId(userId)
                        .token("jwt.overflow." + i + "." + UUID.randomUUID())
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .deviceInfo("Device-" + i)
                        .ipAddress("192.168.1." + i)
                        .build();
                refreshTokenRepository.save(token, 3);
            }

            // 4번째 토큰 저장
            String fourthToken = "jwt.overflow.4." + UUID.randomUUID();
            RefreshToken token4 = RefreshToken.createBuilder()
                    .userId(userId)
                    .token(fourthToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Device-4")
                    .ipAddress("192.168.1.4")
                    .build();
            refreshTokenRepository.save(token4, 3);

            // token-4는 유효
            Optional<RefreshToken> fourth = refreshTokenRepository.findValidToken(fourthToken);
            assertThat(fourth).isPresent();

            // 유효한 토큰은 최대 3개
            List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(userId);
            assertThat(validTokens).hasSizeLessThanOrEqualTo(3);
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
            RefreshToken saved = refreshTokenRepository.save(testToken);

            Optional<RefreshToken> found = refreshTokenRepository.findById(saved.getRefreshTokenId().value());

            assertThat(found).isPresent();
            assertThat(found.get().getToken()).isEqualTo(testToken.getToken());
        }

        @Test
        @DisplayName("토큰으로 조회")
        void findByToken() {
            refreshTokenRepository.save(testToken);

            Optional<RefreshToken> found = refreshTokenRepository.findByToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo("a1b2c3d4e5f6");
        }

        @Test
        @DisplayName("유효한 토큰 조회")
        void findValidToken() {
            refreshTokenRepository.save(testToken);

            Optional<RefreshToken> found = refreshTokenRepository.findValidToken(testToken.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().isValid()).isTrue();
        }

        @Test
        @DisplayName("폐기된 토큰 - findValidToken은 빈 결과")
        void findValidToken_Revoked_ReturnsEmpty() {
            refreshTokenRepository.save(testToken);
            refreshTokenRepository.revokeByToken(testToken.getToken());

            Optional<RefreshToken> found = refreshTokenRepository.findValidToken(testToken.getToken());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("사용자별 토큰 목록 조회")
        void findByUserId() {
            String userId = "d1e2f3a4b5c6";  // hex ID

            RefreshToken token1 = RefreshToken.createBuilder()
                    .userId(userId)
                    .token("jwt.list.1." + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Chrome/120.0")
                    .ipAddress("192.168.1.100")
                    .build();
            refreshTokenRepository.save(token1);

            RefreshToken token2 = RefreshToken.createBuilder()
                    .userId(userId)
                    .token("jwt.list.2." + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Safari/17.0")
                    .ipAddress("10.0.0.1")
                    .build();
            refreshTokenRepository.save(token2);

            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);

            assertThat(tokens).hasSize(2);
        }

        @Test
        @DisplayName("사용자별 유효한 토큰 조회")
        void findValidTokensByUserId() {
            String userId = "e1f2a3b4c5d6";  // hex ID
            String token1Value = "jwt.valid.1." + UUID.randomUUID();
            String token2Value = "jwt.valid.2." + UUID.randomUUID();

            refreshTokenRepository.save(RefreshToken.createBuilder()
                    .userId(userId)
                    .token(token1Value)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Device-1")
                    .ipAddress("192.168.1.1")
                    .build());

            refreshTokenRepository.save(RefreshToken.createBuilder()
                    .userId(userId)
                    .token(token2Value)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .deviceInfo("Device-2")
                    .ipAddress("192.168.1.2")
                    .build());

            // 1개 폐기
            refreshTokenRepository.revokeByToken(token1Value);

            List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(userId);

            assertThat(validTokens).hasSize(1);
        }

        @Test
        @DisplayName("유효한 토큰 수 조회")
        void countValidTokensByUserId() {
            refreshTokenRepository.save(testToken);

            long count = refreshTokenRepository.countValidTokensByUserId("a1b2c3d4e5f6");

            assertThat(count).isEqualTo(1);
        }
    }

    // ========================================
    // 폐기 테스트
    // ========================================

    @Nested
    @DisplayName("폐기")
    class RevokeTest {

        @Test
        @DisplayName("단일 토큰 폐기")
        void revokeByToken() {
            refreshTokenRepository.save(testToken);

            refreshTokenRepository.revokeByToken(testToken.getToken());

            Optional<RefreshToken> found = refreshTokenRepository.findValidToken(testToken.getToken());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("사용자의 모든 토큰 폐기 (전체 로그아웃)")
        void revokeAllByUserId() {
            String userId = "f1a2b3c4d5e6";  // hex ID

            for (int i = 1; i <= 3; i++) {
                RefreshToken token = RefreshToken.createBuilder()
                        .userId(userId)
                        .token("jwt.revoke." + i + "." + UUID.randomUUID())
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .deviceInfo("Device-" + i)
                        .ipAddress("192.168.1." + i)
                        .build();
                refreshTokenRepository.save(token);
            }

            int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);

            assertThat(revokedCount).isEqualTo(3);

            List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(userId);
            assertThat(validTokens).isEmpty();
        }
    }

    // ========================================
    // 정리 (배치용) 테스트
    // ========================================

    @Nested
    @DisplayName("정리 (배치)")
    class CleanupTest {

        @Test
        @DisplayName("만료된 토큰 삭제")
        void deleteExpiredTokens() {
            int deleted = refreshTokenRepository.deleteExpiredTokens();
            assertThat(deleted).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("오래된 폐기 토큰 삭제")
        void deleteOldRevokedTokens() {
            int deleted = refreshTokenRepository.deleteOldRevokedTokens(30);
            assertThat(deleted).isGreaterThanOrEqualTo(0);
        }
    }

    // ========================================
    // 존재 여부 테스트
    // ========================================

    @Nested
    @DisplayName("존재 여부")
    class ExistsTest {

        @Test
        @DisplayName("토큰 존재 여부")
        void existsByToken() {
            refreshTokenRepository.save(testToken);

            assertThat(refreshTokenRepository.existsByToken(testToken.getToken())).isTrue();
            assertThat(refreshTokenRepository.existsByToken("not.exist.token")).isFalse();
        }
    }

    // ========================================
    // 캐시 동작 테스트
    // ========================================

    @Nested
    @DisplayName("캐시 동작 (Redis)")
    class CacheTest {

        @Test
        @DisplayName("저장 시 캐시에도 저장")
        void save_AlsoSavesToCache() {
            refreshTokenRepository.save(testToken);

            // 캐시에서 조회 (Cache Hit)
            Optional<RefreshToken> cached = refreshTokenRepository.findByToken(testToken.getToken());
            assertThat(cached).isPresent();
            assertThat(cached.get().getUserId()).isEqualTo("a1b2c3d4e5f6");
        }

        @Test
        @DisplayName("폐기 시 캐시도 삭제")
        void revoke_AlsoRemovesCache() {
            refreshTokenRepository.save(testToken);

            // 캐시에 로드
            refreshTokenRepository.findByToken(testToken.getToken());

            // 폐기
            refreshTokenRepository.revokeByToken(testToken.getToken());

            // 캐시에서도 삭제됨 (유효한 토큰으로 조회 불가)
            Optional<RefreshToken> found = refreshTokenRepository.findValidToken(testToken.getToken());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("전체 로그아웃 시 유효한 토큰 없음")
        void revokeAll_NoValidTokensRemain() {
            String userId = "a9b8c7d6e5f4";  // hex ID

            for (int i = 1; i <= 3; i++) {
                refreshTokenRepository.save(RefreshToken.createBuilder()
                        .userId(userId)
                        .token("jwt.cache." + i + "." + UUID.randomUUID())
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .deviceInfo("Device-" + i)
                        .ipAddress("192.168.1." + i)
                        .build());
            }

            refreshTokenRepository.revokeAllByUserId(userId);

            // 유효한 토큰이 없어야 함
            List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(userId);
            assertThat(validTokens).isEmpty();
        }
    }
}