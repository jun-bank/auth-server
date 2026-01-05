package com.jun_bank.auth_server.domain.token.infrastructure.cache;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * RefreshToken 캐시 Repository (Lua Script 기반)
 * <p>
 * Redis Lua Script를 통해 원자적으로 토큰을 관리합니다.
 * 스케일 아웃 환경에서도 동시성 문제가 발생하지 않습니다.
 *
 * <h3>원자적 처리:</h3>
 * <ul>
 *   <li>같은 디바이스 토큰 → 교체</li>
 *   <li>다른 디바이스 토큰 → 추가</li>
 *   <li>최대 세션 초과 → 가장 오래된 것 삭제</li>
 * </ul>
 *
 * <h3>Redis 키 구조:</h3>
 * <pre>
 * refresh-token:user:{userId}          - ZSET (토큰 목록, score=생성시간)
 * refresh-token:data:{token}           - HASH (토큰 상세 데이터)
 * </pre>
 *
 * <h3>읽기/쓰기 분리:</h3>
 * <ul>
 *   <li>쓰기 (Lua Script): Primary</li>
 *   <li>읽기: Replica 우선</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenCacheRepository {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<String> refreshTokenSaveScript;
    private final DefaultRedisScript<Long> refreshTokenRevokeScript;
    private final DefaultRedisScript<Long> refreshTokenRevokeAllScript;

    // Jackson 3 - JsonMapper
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private static final String USER_TOKENS_PREFIX = "refresh-token:user:";
    private static final String TOKEN_DATA_PREFIX = "refresh-token:data:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private static final int DEFAULT_MAX_SESSIONS = 5;

    // ========================================
    // 저장 (Lua Script - 원자적)
    // ========================================

    /**
     * RefreshToken 저장 (원자적)
     * <p>
     * Lua Script를 통해 다음을 원자적으로 처리:
     * <ol>
     *   <li>같은 디바이스의 기존 토큰 → 교체</li>
     *   <li>다른 디바이스 → 추가</li>
     *   <li>최대 세션 초과 → 가장 오래된 것 삭제</li>
     * </ol>
     * </p>
     *
     * @param refreshToken 저장할 토큰
     * @param deviceId     디바이스 식별자 (User-Agent 해시 등)
     * @return SaveResult (상태, 삭제된 토큰 정보)
     */
    public SaveResult save(RefreshToken refreshToken, String deviceId) {
        return save(refreshToken, deviceId, DEFAULT_MAX_SESSIONS);
    }

    /**
     * RefreshToken 저장 (최대 세션 수 지정)
     *
     * @param refreshToken 저장할 토큰
     * @param deviceId     디바이스 식별자
     * @param maxSessions  최대 허용 세션 수
     * @return SaveResult
     */
    public SaveResult save(RefreshToken refreshToken, String deviceId, int maxSessions) {
        String userTokensKey = USER_TOKENS_PREFIX + refreshToken.getUserId();
        String tokenDataPrefix = TOKEN_DATA_PREFIX;

        try {
            String tokenDataJson = JSON_MAPPER.writeValueAsString(refreshToken);
            long createdAt = refreshToken.getCreatedAt() != null
                    ? refreshToken.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
                    : System.currentTimeMillis() / 1000;
            long ttlSeconds = calculateTtlSeconds(refreshToken.getExpiresAt());

            List<String> keys = Arrays.asList(userTokensKey, tokenDataPrefix);
            String resultJson = stringRedisTemplate.execute(
                    refreshTokenSaveScript,
                    keys,
                    refreshToken.getUserId(),
                    refreshToken.getToken(),
                    deviceId,
                    tokenDataJson,
                    String.valueOf(createdAt),
                    String.valueOf(ttlSeconds),
                    String.valueOf(maxSessions)
            );

            return parseSaveResult(resultJson);

        } catch (Exception e) {
            log.error("RefreshToken 캐시 저장 실패: userId={}, error={}", refreshToken.getUserId(), e.getMessage());
            return SaveResult.error(e.getMessage());
        }
    }

    // ========================================
    // 조회 (Replica에서 읽기)
    // ========================================

    /**
     * 토큰으로 RefreshToken 조회
     *
     * @param token JWT 토큰 값
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> findByToken(String token) {
        String tokenKey = TOKEN_DATA_PREFIX + token;
        try {
            Map<Object, Object> data = redisTemplate.opsForHash().entries(tokenKey);
            if (data.isEmpty()) {
                return Optional.empty();
            }

            String tokenDataJson = (String) data.get("data");
            if (tokenDataJson == null) {
                return Optional.empty();
            }

            RefreshToken refreshToken = JSON_MAPPER.readValue(tokenDataJson, RefreshToken.class);
            log.debug("RefreshToken 캐시 히트: token={}...", token.substring(0, Math.min(10, token.length())));
            return Optional.of(refreshToken);

        } catch (Exception e) {
            log.warn("RefreshToken 캐시 조회 실패: error={}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 유효한 토큰 조회 (만료/폐기 확인)
     *
     * @param token JWT 토큰 값
     * @return Optional<RefreshToken> (유효한 경우만)
     */
    public Optional<RefreshToken> findValidToken(String token) {
        return findByToken(token)
                .filter(RefreshToken::isValid);
    }

    /**
     * 사용자의 모든 토큰 조회
     *
     * @param userId User Service의 사용자 ID
     * @return List<RefreshToken>
     */
    public List<RefreshToken> findByUserId(String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        try {
            Set<String> tokens = stringRedisTemplate.opsForZSet().range(userTokensKey, 0, -1);
            if (tokens == null || tokens.isEmpty()) {
                return Collections.emptyList();
            }

            List<RefreshToken> result = new ArrayList<>();
            for (String token : tokens) {
                findByToken(token).ifPresent(result::add);
            }
            return result;

        } catch (Exception e) {
            log.warn("사용자 토큰 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 사용자의 유효한 토큰 수 조회
     *
     * @param userId User Service의 사용자 ID
     * @return 유효한 토큰 수
     */
    public long countByUserId(String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(userTokensKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.warn("토큰 수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0L;
        }
    }

    // ========================================
    // 폐기 (Lua Script - 원자적)
    // ========================================

    /**
     * 단일 토큰 폐기 (원자적)
     *
     * @param token  폐기할 토큰
     * @param userId 사용자 ID
     * @return 폐기 성공 여부
     */
    public boolean revoke(String token, String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        String tokenDataPrefix = TOKEN_DATA_PREFIX;

        try {
            List<String> keys = Arrays.asList(userTokensKey, tokenDataPrefix);
            Long result = stringRedisTemplate.execute(
                    refreshTokenRevokeScript,
                    keys,
                    token
            );

            boolean success = result != null && result == 1L;
            if (success) {
                log.debug("RefreshToken 캐시 폐기 성공: token={}...", token.substring(0, Math.min(10, token.length())));
            }
            return success;

        } catch (Exception e) {
            log.error("RefreshToken 캐시 폐기 실패: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 사용자의 모든 토큰 폐기 (원자적)
     * <p>
     * 전체 로그아웃 또는 비밀번호 변경 시 사용
     * </p>
     *
     * @param userId 사용자 ID
     * @return 폐기된 토큰 수
     */
    public long revokeAll(String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        String tokenDataPrefix = TOKEN_DATA_PREFIX;

        try {
            List<String> keys = Arrays.asList(userTokensKey, tokenDataPrefix);
            Long result = stringRedisTemplate.execute(
                    refreshTokenRevokeAllScript,
                    keys
            );

            long count = result != null ? result : 0L;
            log.info("RefreshToken 캐시 전체 폐기: userId={}, count={}", userId, count);
            return count;

        } catch (Exception e) {
            log.error("RefreshToken 캐시 전체 폐기 실패: userId={}, error={}", userId, e.getMessage());
            return 0L;
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private long calculateTtlSeconds(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return DEFAULT_TTL.toSeconds();
        }
        long seconds = Duration.between(LocalDateTime.now(), expiresAt).toSeconds();
        return Math.max(1, seconds);
    }

    private SaveResult parseSaveResult(String json) {
        if (json == null) {
            return SaveResult.error("Null response from Redis");
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            SaveStatus status = SaveStatus.fromString(node.get("status").asText());
            String removedToken = node.has("removedToken") && !node.get("removedToken").isNull()
                    ? node.get("removedToken").asText()
                    : null;
            return new SaveResult(status, removedToken, null);
        } catch (Exception e) {
            return SaveResult.error("Failed to parse result: " + e.getMessage());
        }
    }

    // ========================================
    // Result DTOs
    // ========================================

    /**
     * 토큰 저장 상태
     */
    public enum SaveStatus {
        /** 신규 생성 */
        CREATED,
        /** 기존 토큰 교체 (같은 디바이스) */
        REPLACED,
        /** 최대 세션 초과로 가장 오래된 토큰 삭제 */
        OVERFLOW,
        /** 에러 발생 */
        ERROR;

        /**
         * 문자열에서 변환 (Lua Script 결과 파싱용)
         */
        public static SaveStatus fromString(String value) {
            if (value == null) {
                return ERROR;
            }
            return switch (value) {
                case "CREATED" -> CREATED;
                case "REPLACED" -> REPLACED;
                case "OVERFLOW" -> OVERFLOW;
                default -> ERROR;
            };
        }

        public boolean isSuccess() {
            return this != ERROR;
        }
    }

    /**
     * 토큰 저장 결과
     */
    public record SaveResult(
            SaveStatus status,
            String removedToken,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return status.isSuccess();
        }

        public boolean isReplaced() {
            return status == SaveStatus.REPLACED;
        }

        public boolean isOverflow() {
            return status == SaveStatus.OVERFLOW;
        }

        public static SaveResult error(String message) {
            return new SaveResult(SaveStatus.ERROR, null, message);
        }
    }
}