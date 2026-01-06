package com.jun_bank.auth_server.global.infrastructure.cache;

import com.jun_bank.auth_server.global.infrastructure.cache.IpRateLimitCacheRepository.RateLimitResult;
import com.jun_bank.auth_server.global.infrastructure.cache.IpRateLimitCacheRepository.RateLimitStatus;
import com.jun_bank.auth_server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("IpRateLimitCacheRepository 통합 테스트")
class IpRateLimitCacheRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private IpRateLimitCacheRepository ipRateLimitCacheRepository;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    // flushAll 제거 - Lettuce 연결 풀 캐싱 문제로 인해
    // 각 테스트에서 고유한 IP/키(UUID)를 사용하여 충돌 방지

    // ========================================
    // 로그인 Rate Limit 테스트
    // ========================================

    @Test
    @DisplayName("로그인 정상 요청 - 허용")
    void checkLoginRateLimit_Allowed() {
        String ip = "192.168.1.1";

        RateLimitResult result = ipRateLimitCacheRepository.checkLoginRateLimit(ip);

        // 디버깅: currentCount가 0이면 Lua Script 에러
        System.out.println("=== DEBUG: checkLoginRateLimit_Allowed ===");
        System.out.println("result.isAllowed()=" + result.isAllowed());
        System.out.println("result.currentCount()=" + result.currentCount());
        System.out.println("result.status()=" + result.status());
        System.out.println("result.reason()=" + result.reason());

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.status()).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("로그인 요청 카운트 증가")
    void checkLoginRateLimit_CountIncreases() {
        String ip = "192.168.1.2";

        ipRateLimitCacheRepository.checkLoginRateLimit(ip);
        ipRateLimitCacheRepository.checkLoginRateLimit(ip);
        RateLimitResult result = ipRateLimitCacheRepository.checkLoginRateLimit(ip);

        assertThat(result.currentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("로그인 Rate Limit 초과 시 차단")
    void checkLoginRateLimit_ExceedsMax_Blocked() {
        String ip = "192.168.1.3";

        // 50회 요청
        for (int i = 0; i < 50; i++) {
            ipRateLimitCacheRepository.checkLoginRateLimit(ip);
        }

        // 51회차 - 차단
        RateLimitResult result = ipRateLimitCacheRepository.checkLoginRateLimit(ip);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.currentCount()).isEqualTo(51);
    }

    // ========================================
    // API Rate Limit 테스트
    // ========================================

    @Test
    @DisplayName("API 정상 요청 - 허용")
    void checkApiRateLimit_Allowed() {
        String ip = "192.168.2.1";

        RateLimitResult result = ipRateLimitCacheRepository.checkApiRateLimit(ip);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.currentCount()).isEqualTo(1);
    }

    // ========================================
    // 커스텀 Rate Limit 테스트
    // ========================================

    @Test
    @DisplayName("커스텀 Rate Limit 초과 시 자동 차단")
    void checkRateLimit_ExceedsMax_AutoBlock() {
        String key = "custom:192.168.3.1";

        // 5회 허용
        for (int i = 0; i < 5; i++) {
            RateLimitResult r = ipRateLimitCacheRepository.checkRateLimit(key, 5, 60, 300);
            assertThat(r.isAllowed()).isTrue();
        }

        // 6회차 차단
        RateLimitResult result = ipRateLimitCacheRepository.checkRateLimit(key, 5, 60, 300);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).contains("Rate limit exceeded");
    }

    // ========================================
    // IP 차단 관리 테스트
    // ========================================

    @Test
    @DisplayName("수동 IP 차단")
    void blockIp_CheckWithIsBlocked() {
        String ip = "10.0.0.1";

        ipRateLimitCacheRepository.blockIp(ip, "Suspicious activity", 3600);

        assertThat(ipRateLimitCacheRepository.isBlocked(ip)).isTrue();
    }

    @Test
    @DisplayName("IP 차단 해제")
    void unblockIp() {
        String ip = "10.0.0.2";

        ipRateLimitCacheRepository.blockIp(ip, "Test block", 3600);
        assertThat(ipRateLimitCacheRepository.isBlocked(ip)).isTrue();

        ipRateLimitCacheRepository.unblockIp(ip);

        assertThat(ipRateLimitCacheRepository.isBlocked(ip)).isFalse();
    }

    @Test
    @DisplayName("차단 안된 IP 확인")
    void isBlocked_NotBlocked() {
        String ip = "10.0.0.3";

        assertThat(ipRateLimitCacheRepository.isBlocked(ip)).isFalse();
    }

    // ========================================
    // Rate Limit 키 분리 테스트
    // ========================================

    @Test
    @DisplayName("로그인과 API Rate Limit은 독립적")
    void loginAndApiRateLimit_Independent() {
        String ip = "172.16.1.1";

        // 로그인 3회
        for (int i = 0; i < 3; i++) {
            ipRateLimitCacheRepository.checkLoginRateLimit(ip);
        }

        // API는 첫 요청
        RateLimitResult apiResult = ipRateLimitCacheRepository.checkApiRateLimit(ip);

        assertThat(apiResult.currentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 IP는 독립적으로 카운팅")
    void differentIps_IndependentCounting() {
        String ip1 = "172.16.1.2";
        String ip2 = "172.16.1.3";

        // IP1에서 3회
        for (int i = 0; i < 3; i++) {
            ipRateLimitCacheRepository.checkLoginRateLimit(ip1);
        }

        // IP2는 첫 요청
        RateLimitResult result = ipRateLimitCacheRepository.checkLoginRateLimit(ip2);

        assertThat(result.currentCount()).isEqualTo(1);
    }

    // ========================================
    // 원자성 테스트
    // ========================================

    @Test
    @DisplayName("동시 요청에도 정확한 카운팅")
    void atomicCounting() throws InterruptedException {
        String key = "atomic:172.16.2.1";
        int threadCount = 10;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() ->
                    ipRateLimitCacheRepository.checkRateLimit(key, 100, 60, 300)
            );
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        RateLimitResult result = ipRateLimitCacheRepository.checkRateLimit(key, 100, 60, 300);

        assertThat(result.currentCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("차단과 카운팅 원자적 처리")
    void atomicBlockAndCount() {
        String key = "atomic-block:172.16.2.2";

        // 3회 허용
        for (int i = 0; i < 3; i++) {
            ipRateLimitCacheRepository.checkRateLimit(key, 3, 60, 300);
        }

        // 4회차 차단
        RateLimitResult result = ipRateLimitCacheRepository.checkRateLimit(key, 3, 60, 300);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.currentCount()).isEqualTo(4);
    }

    // ========================================
    // RateLimitResult 테스트
    // ========================================

    @Test
    @DisplayName("허용 결과 메서드 동작")
    void allowedResult() {
        String ip = "192.168.99.1";

        RateLimitResult result = ipRateLimitCacheRepository.checkLoginRateLimit(ip);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isDenied()).isFalse();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRemainingMinutes()).isZero();
    }

    @Test
    @DisplayName("차단된 결과")
    void blockedByRateLimit() {
        String key = "blocked-result:192.168.99.2";

        // 3회 허용
        for (int i = 0; i < 3; i++) {
            ipRateLimitCacheRepository.checkRateLimit(key, 3, 60, 300);
        }

        // 4회차 차단
        RateLimitResult result = ipRateLimitCacheRepository.checkRateLimit(key, 3, 60, 300);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isDenied()).isTrue();
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getRemainingMinutes()).isGreaterThanOrEqualTo(0);
    }
}