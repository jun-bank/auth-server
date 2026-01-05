package com.jun_bank.auth_server.domain.auth.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

/**
 * 로그인 시도 캐시 Repository (Lua Script 기반)
 * <p>
 * Redis Lua Script를 통해 원자적으로 로그인 시도를 카운팅합니다.
 * 스케일 아웃 환경에서 동시성 문제 없이 정확한 카운팅이 보장됩니다.
 *
 * <h3>원자적 처리:</h3>
 * <ul>
 *   <li>INCREMENT: 실패 카운터 증가 + 잠금 판단</li>
 *   <li>RESET: 성공 시 카운터 초기화</li>
 *   <li>CHECK: 현재 상태 확인</li>
 * </ul>
 *
 * <h3>Redis 키 구조:</h3>
 * <pre>
 * login-attempt:email:{email}    - 로그인 시도 카운터
 * login-attempt:lock:{email}     - 계정 잠금 상태
 * </pre>
 *
 * <h3>Race Condition 방지:</h3>
 * <pre>
 * Server 1: 실패 5회 ──┐
 *                      ├── Lua Script (Single Thread) ── 정확히 5회로 잠금
 * Server 2: 실패 5회 ──┘
 * </pre>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LoginAttemptCacheRepository {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final DefaultRedisScript<String> loginAttemptScript;

    // Jackson 3 - JsonMapper
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private static final String ATTEMPT_PREFIX = "login-attempt:email:";
    private static final String LOCK_PREFIX = "login-attempt:lock:";

    // 기본 설정
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_LOCK_SECONDS = 30 * 60;  // 30분
    private static final int DEFAULT_ATTEMPT_TTL = 30 * 60;   // 30분

    // ========================================
    // 로그인 시도 관리 (Lua Script - 원자적)
    // ========================================

    /**
     * 로그인 실패 기록 (원자적)
     * <p>
     * 실패 카운터를 증가시키고, 최대 횟수 초과 시 자동 잠금합니다.
     * </p>
     *
     * @param email 이메일
     * @return AttemptResult (상태, 시도 횟수, 남은 잠금 시간)
     */
    public AttemptResult recordFailure(String email) {
        return recordFailure(email, DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_SECONDS);
    }

    /**
     * 로그인 실패 기록 (설정 지정)
     *
     * @param email       이메일
     * @param maxAttempts 최대 시도 횟수
     * @param lockSeconds 잠금 시간 (초)
     * @return AttemptResult
     */
    public AttemptResult recordFailure(String email, int maxAttempts, int lockSeconds) {
        return executeScript(email, "INCREMENT", maxAttempts, lockSeconds, DEFAULT_ATTEMPT_TTL);
    }

    /**
     * 로그인 성공 기록 (원자적)
     * <p>
     * 성공 시 시도 카운터와 잠금 상태를 초기화합니다.
     * </p>
     *
     * @param email 이메일
     * @return AttemptResult
     */
    public AttemptResult recordSuccess(String email) {
        return executeScript(email, "RESET", DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_SECONDS, DEFAULT_ATTEMPT_TTL);
    }

    /**
     * 현재 상태 확인 (원자적)
     *
     * @param email 이메일
     * @return AttemptResult
     */
    public AttemptResult checkStatus(String email) {
        return executeScript(email, "CHECK", DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_SECONDS, DEFAULT_ATTEMPT_TTL);
    }

    /**
     * 잠금 여부 확인
     *
     * @param email 이메일
     * @return 잠금 상태면 true
     */
    public boolean isLocked(String email) {
        AttemptResult result = checkStatus(email);
        return result.isLocked();
    }

    /**
     * 현재 실패 횟수 조회
     *
     * @param email 이메일
     * @return 실패 횟수
     */
    public int getFailedAttempts(String email) {
        AttemptResult result = checkStatus(email);
        return result.attempts();
    }

    /**
     * 남은 잠금 시간 조회 (초)
     *
     * @param email 이메일
     * @return 남은 시간 (초), 잠금 상태가 아니면 0
     */
    public long getRemainingLockSeconds(String email) {
        AttemptResult result = checkStatus(email);
        return result.remainingSeconds();
    }

    // ========================================
    // Internal - Lua Script 실행
    // ========================================

    private AttemptResult executeScript(String email, String action, int maxAttempts, int lockSeconds, int attemptTtl) {
        String attemptKey = ATTEMPT_PREFIX + email;
        String lockKey = LOCK_PREFIX + email;

        try {
            List<String> keys = Arrays.asList(attemptKey, lockKey);
            String resultJson = stringRedisTemplate.execute(
                    loginAttemptScript,
                    keys,
                    action,
                    String.valueOf(maxAttempts),
                    String.valueOf(lockSeconds),
                    String.valueOf(attemptTtl)
            );

            return parseAttemptResult(resultJson, email);

        } catch (Exception e) {
            log.error("로그인 시도 처리 실패: email={}, action={}, error={}", email, action, e.getMessage());
            return AttemptResult.error(email, e.getMessage());
        }
    }

    private AttemptResult parseAttemptResult(String json, String email) {
        if (json == null) {
            return AttemptResult.error(email, "Null response from Redis");
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            AttemptStatus status = AttemptStatus.fromString(node.get("status").asText());
            int attempts = node.get("attempts").asInt();
            long remainingSeconds = node.get("remainingSeconds").asLong();

            return new AttemptResult(email, status, attempts, remainingSeconds, null);

        } catch (Exception e) {
            return AttemptResult.error(email, "Failed to parse result: " + e.getMessage());
        }
    }

    // ========================================
    // Result DTO
    // ========================================

    /**
     * 로그인 시도 상태
     */
    public enum AttemptStatus {
        /** 정상 */
        OK,
        /** 이번 요청으로 잠금됨 */
        LOCKED,
        /** 이미 잠금 상태 */
        ALREADY_LOCKED,
        /** 알 수 없는 액션 */
        UNKNOWN_ACTION,
        /** 에러 발생 */
        ERROR;

        /**
         * 문자열에서 변환 (Lua Script 결과 파싱용)
         */
        public static AttemptStatus fromString(String value) {
            if (value == null) {
                return ERROR;
            }
            return switch (value) {
                case "OK" -> OK;
                case "LOCKED" -> LOCKED;
                case "ALREADY_LOCKED" -> ALREADY_LOCKED;
                case "UNKNOWN_ACTION" -> UNKNOWN_ACTION;
                default -> ERROR;
            };
        }

        /**
         * 잠금 상태인지 확인
         */
        public boolean isLocked() {
            return this == LOCKED || this == ALREADY_LOCKED;
        }
    }

    /**
     * 로그인 시도 결과
     *
     * @param email            이메일
     * @param status           상태 (OK, LOCKED, ALREADY_LOCKED, ERROR)
     * @param attempts         현재 실패 횟수
     * @param remainingSeconds 남은 잠금 시간 (초)
     * @param errorMessage     에러 메시지 (ERROR 시)
     */
    public record AttemptResult(
            String email,
            AttemptStatus status,
            int attempts,
            long remainingSeconds,
            String errorMessage
    ) {
        /**
         * 잠금 상태인지 확인
         */
        public boolean isLocked() {
            return status.isLocked();
        }

        /**
         * 성공 (에러 아님)
         */
        public boolean isSuccess() {
            return status != AttemptStatus.ERROR;
        }

        /**
         * 방금 잠금되었는지 (이번 요청으로 인해)
         */
        public boolean justLocked() {
            return status == AttemptStatus.LOCKED;
        }

        /**
         * 남은 잠금 시간 (분)
         */
        public long getRemainingMinutes() {
            return remainingSeconds / 60;
        }

        /**
         * 에러 결과 생성
         */
        public static AttemptResult error(String email, String message) {
            return new AttemptResult(email, AttemptStatus.ERROR, 0, 0, message);
        }
    }
}