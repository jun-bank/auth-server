package com.jun_bank.auth_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Auth Server 고유 설정 프로퍼티
 * <p>
 * application.yml의 auth-server.* 설정을 바인딩합니다.
 *
 * <h3>설정 예시 (auth-server.yml):</h3>
 * <pre>
 * auth-server:
 *   max-concurrent-sessions: 3
 *   refresh-token-reuse: false
 *   login-failure-lock-threshold: 5
 *   account-lock-duration-minutes: 30
 * </pre>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class LoginService {
 *     private final AuthServerProperties authServerProperties;
 *
 *     public void recordLoginFailure(AuthUser authUser) {
 *         authUser.recordLoginFailure(
 *             authServerProperties.getLoginFailureLockThreshold(),
 *             authServerProperties.getAccountLockDurationMinutes()
 *         );
 *     }
 * }
 * }</pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth-server")
public class AuthServerProperties {

  /**
   * 동시 로그인 허용 수
   * <p>
   * 하나의 계정으로 동시에 로그인할 수 있는 세션(디바이스) 수입니다.
   * 초과 시 가장 오래된 세션의 Refresh Token이 폐기됩니다.
   * </p>
   * <ul>
   *   <li>0: 무제한</li>
   *   <li>1: 단일 세션만 허용 (다른 기기에서 로그인 시 기존 세션 종료)</li>
   *   <li>3 (기본값): 최대 3개 세션</li>
   * </ul>
   */
  private int maxConcurrentSessions = 3;

  /**
   * 리프레시 토큰 재사용 허용 여부
   * <p>
   * Refresh Token Rotation 정책을 결정합니다.
   * </p>
   * <ul>
   *   <li>false (기본값, 권장): 1회 사용 후 새 토큰 발급 (Rotation)</li>
   *   <li>true: 만료 시까지 재사용 가능</li>
   * </ul>
   *
   * <h4>Rotation의 장점:</h4>
   * <ul>
   *   <li>토큰 탈취 시 피해 최소화</li>
   *   <li>탈취된 토큰 사용 시 즉시 감지 가능</li>
   * </ul>
   */
  private boolean refreshTokenReuse = false;

  /**
   * 로그인 실패 시 계정 잠금 임계값
   * <p>
   * 이 횟수만큼 연속으로 로그인 실패 시 계정이 잠깁니다.
   * </p>
   * <ul>
   *   <li>기본값: 5회</li>
   *   <li>0: 잠금 기능 비활성화</li>
   * </ul>
   */
  private int loginFailureLockThreshold = 5;

  /**
   * 계정 잠금 시간 (분)
   * <p>
   * 계정이 잠긴 후 이 시간이 지나면 자동으로 해제됩니다.
   * 관리자가 수동으로 해제할 수도 있습니다.
   * </p>
   * <ul>
   *   <li>기본값: 30분</li>
   * </ul>
   */
  private int accountLockDurationMinutes = 30;
}