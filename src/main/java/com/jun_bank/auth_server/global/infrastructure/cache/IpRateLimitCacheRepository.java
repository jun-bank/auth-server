package com.jun_bank.auth_server.global.infrastructure.cache;

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
 * IP Rate Limiting 캐시 Repository (Lua Script 기반)
 * <p>
 * Redis Lua Script를 통해 원자적으로 IP 기반 요청 제한을 처리합니다.
 * 무차별 대입 공격, DDoS 방어에 사용됩니다.
 *
 * <h3>원자적 처리:</h3>
 * <ul>
 *   <li>CHECK_AND_INCREMENT: 요청 확인 + 카운트 증가 + 자동 차단</li>
 *   <li>BLOCK: 수동 IP 차단</li>
 *   <li>UNBLOCK: IP 차단 해제</li>
 *   <li>IS_BLOCKED: 차단 여부 확인</li>
 * </ul>
 *
 * <h3>Redis 키 구조:</h3>
 * <pre>
 * ip-rate:counter:{ip}    - 요청 카운터 (Sliding Window)
 * ip-rate:block:{ip}      - IP 차단 상태
 * </pre>
 *
 * <h3>차단 정책:</h3>
 * <ul>
 *   <li>로그인: 10분간 50회 초과 → 1시간 차단</li>
 *   <li>API: 1분간 100회 초과 → 10분 차단</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class IpRateLimitCacheRepository {

  private final RedisTemplate<String, String> stringRedisTemplate;
  private final DefaultRedisScript<String> ipRateLimitScript;

  // Jackson 3 - JsonMapper
  private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
          .findAndAddModules()
          .build();

  private static final String COUNTER_PREFIX = "ip-rate:counter:";
  private static final String BLOCK_PREFIX = "ip-rate:block:";

  // 로그인 Rate Limit 기본값
  private static final int LOGIN_MAX_REQUESTS = 50;      // 50회
  private static final int LOGIN_WINDOW_SECONDS = 600;   // 10분
  private static final int LOGIN_BLOCK_SECONDS = 3600;   // 1시간

  // API Rate Limit 기본값
  private static final int API_MAX_REQUESTS = 100;       // 100회
  private static final int API_WINDOW_SECONDS = 60;      // 1분
  private static final int API_BLOCK_SECONDS = 600;      // 10분

  // ========================================
  // 로그인 Rate Limiting
  // ========================================

  /**
   * 로그인 요청 Rate Limit 체크 (원자적)
   * <p>
   * 10분간 50회 초과 시 1시간 차단
   * </p>
   *
   * @param ipAddress IP 주소
   * @return RateLimitResult
   */
  public RateLimitResult checkLoginRateLimit(String ipAddress) {
    return checkRateLimit(
            "login:" + ipAddress,
            LOGIN_MAX_REQUESTS,
            LOGIN_WINDOW_SECONDS,
            LOGIN_BLOCK_SECONDS
    );
  }

  // ========================================
  // API Rate Limiting
  // ========================================

  /**
   * API 요청 Rate Limit 체크 (원자적)
   * <p>
   * 1분간 100회 초과 시 10분 차단
   * </p>
   *
   * @param ipAddress IP 주소
   * @return RateLimitResult
   */
  public RateLimitResult checkApiRateLimit(String ipAddress) {
    return checkRateLimit(
            "api:" + ipAddress,
            API_MAX_REQUESTS,
            API_WINDOW_SECONDS,
            API_BLOCK_SECONDS
    );
  }

  // ========================================
  // 일반 Rate Limiting
  // ========================================

  /**
   * 커스텀 Rate Limit 체크 (원자적)
   *
   * @param key           Rate Limit 키
   * @param maxRequests   최대 요청 수
   * @param windowSeconds 시간 윈도우 (초)
   * @param blockSeconds  차단 시간 (초)
   * @return RateLimitResult
   */
  public RateLimitResult checkRateLimit(String key, int maxRequests, int windowSeconds, int blockSeconds) {
    return executeScript(key, "CHECK_AND_INCREMENT", maxRequests, windowSeconds, blockSeconds, null);
  }

  // ========================================
  // IP 차단 관리
  // ========================================

  /**
   * IP 차단 여부 확인
   *
   * @param ipAddress IP 주소
   * @return 차단 상태면 true
   */
  public boolean isBlocked(String ipAddress) {
    RateLimitResult result = executeScript(
            ipAddress, "IS_BLOCKED", 0, 0, 0, null
    );
    return result.isBlocked();
  }

  /**
   * IP 수동 차단
   *
   * @param ipAddress    IP 주소
   * @param reason       차단 사유
   * @param blockSeconds 차단 시간 (초)
   */
  public void blockIp(String ipAddress, String reason, int blockSeconds) {
    executeScript(ipAddress, "BLOCK", 0, 0, blockSeconds, reason);
    log.warn("IP 수동 차단: ip={}, reason={}, duration={}초", ipAddress, reason, blockSeconds);
  }

  /**
   * IP 차단 해제
   *
   * @param ipAddress IP 주소
   */
  public void unblockIp(String ipAddress) {
    executeScript(ipAddress, "UNBLOCK", 0, 0, 0, null);
    log.info("IP 차단 해제: ip={}", ipAddress);
  }

  // ========================================
  // Internal - Lua Script 실행
  // ========================================

  private RateLimitResult executeScript(String key, String action, int maxRequests,
                                        int windowSeconds, int blockSeconds, String reason) {
    String counterKey = COUNTER_PREFIX + key;
    String blockKey = BLOCK_PREFIX + key;

    try {
      List<String> keys = Arrays.asList(counterKey, blockKey);
      String resultJson = stringRedisTemplate.execute(
              ipRateLimitScript,
              keys,
              action,
              String.valueOf(maxRequests),
              String.valueOf(windowSeconds),
              String.valueOf(blockSeconds),
              reason != null ? reason : ""
      );

      return parseResult(resultJson, key);

    } catch (Exception e) {
      log.error("IP Rate Limit 처리 실패: key={}, action={}, error={}", key, action, e.getMessage());
      // 에러 시 보수적으로 허용 (서비스 중단 방지)
      return RateLimitResult.allowed(key);
    }
  }

  private RateLimitResult parseResult(String json, String key) {
    if (json == null) {
      return RateLimitResult.allowed(key);
    }
    try {
      JsonNode node = JSON_MAPPER.readTree(json);
      boolean allowed = node.get("allowed").asBoolean();
      int currentCount = node.get("currentCount").asInt();
      boolean blocked = node.get("blocked").asBoolean();
      long remainingSeconds = node.get("remainingSeconds").asLong();
      String reason = node.has("reason") && !node.get("reason").isNull()
              ? node.get("reason").asText()
              : null;

      RateLimitStatus status = blocked ? RateLimitStatus.BLOCKED
              : (allowed ? RateLimitStatus.ALLOWED : RateLimitStatus.BLOCKED);

      return new RateLimitResult(key, status, currentCount, remainingSeconds, reason);

    } catch (Exception e) {
      log.warn("Rate Limit 결과 파싱 실패: key={}, error={}", key, e.getMessage());
      return RateLimitResult.allowed(key);
    }
  }

  // ========================================
  // Result DTO
  // ========================================

  /**
   * Rate Limit 상태
   */
  public enum RateLimitStatus {
    /** 허용 */
    ALLOWED,
    /** 차단됨 */
    BLOCKED,
    /** 에러 */
    ERROR;

    public boolean isAllowed() {
      return this == ALLOWED;
    }

    public boolean isBlocked() {
      return this == BLOCKED;
    }
  }

  /**
   * Rate Limit 결과
   *
   * @param key              Rate Limit 키
   * @param status           상태 (ALLOWED, BLOCKED, ERROR)
   * @param currentCount     현재 요청 수
   * @param remainingSeconds 남은 차단 시간 (초)
   * @param reason           차단 사유
   */
  public record RateLimitResult(
          String key,
          RateLimitStatus status,
          int currentCount,
          long remainingSeconds,
          String reason
  ) {
    /**
     * 요청 허용 여부
     */
    public boolean isAllowed() {
      return status.isAllowed();
    }

    /**
     * 차단되어 요청이 거부됨
     */
    public boolean isDenied() {
      return !isAllowed();
    }

    /**
     * 차단 상태
     */
    public boolean isBlocked() {
      return status.isBlocked();
    }

    /**
     * 남은 차단 시간 (분)
     */
    public long getRemainingMinutes() {
      return remainingSeconds / 60;
    }

    /**
     * 허용 결과 생성 (에러 시 기본값)
     */
    public static RateLimitResult allowed(String key) {
      return new RateLimitResult(key, RateLimitStatus.ALLOWED, 0, 0, null);
    }
  }
}