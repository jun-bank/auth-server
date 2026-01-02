package com.jun_bank.auth_server.global.infrastructure.cache;

/**
 * 캐시 이름 상수 정의
 * <p>
 * Auth Server에서 사용하는 모든 캐시 이름을 중앙 관리합니다.
 * @Cacheable, @CacheEvict 등에서 사용합니다.
 *
 * <h3>캐시 목록:</h3>
 * <table border="1">
 *   <tr><th>캐시</th><th>TTL</th><th>용도</th></tr>
 *   <tr><td>auth-user</td><td>30분</td><td>인증 사용자 정보</td></tr>
 *   <tr><td>refresh-token</td><td>7일</td><td>리프레시 토큰</td></tr>
 *   <tr><td>login-attempt</td><td>30분</td><td>로그인 시도 횟수</td></tr>
 *   <tr><td>ip-block</td><td>1시간</td><td>IP 차단 목록</td></tr>
 * </table>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @Cacheable(cacheNames = CacheNames.AUTH_USER, key = "'email:' + #email")
 * public AuthUser findByEmail(String email) { ... }
 *
 * @CacheEvict(cacheNames = CacheNames.AUTH_USER, key = "'email:' + #email")
 * public void updateAuthUser(String email, AuthUser authUser) { ... }
 * }</pre>
 */
public final class CacheNames {

  private CacheNames() {
    // 인스턴스화 방지
  }

  // ========================================
  // 인증 사용자 관련
  // ========================================

  /**
   * 인증 사용자 캐시
   * <p>
   * TTL: 30분<br>
   * Key 패턴: auth-user::email:{email}, auth-user::userId:{userId}
   * </p>
   */
  public static final String AUTH_USER = "auth-user";

  // ========================================
  // 토큰 관련
  // ========================================

  /**
   * 리프레시 토큰 캐시
   * <p>
   * TTL: 7일<br>
   * Key 패턴: refresh-token::token:{token}, refresh-token::userId:{userId}
   * </p>
   */
  public static final String REFRESH_TOKEN = "refresh-token";

  // ========================================
  // 보안 관련
  // ========================================

  /**
   * 로그인 시도 횟수 캐시
   * <p>
   * TTL: 30분<br>
   * Key 패턴: login-attempt::email:{email}
   * </p>
   *
   * <h4>용도:</h4>
   * <ul>
   *   <li>로그인 실패 횟수 추적</li>
   *   <li>계정 잠금 판단</li>
   *   <li>무차별 대입 공격 방어</li>
   * </ul>
   */
  public static final String LOGIN_ATTEMPT = "login-attempt";

  /**
   * IP 차단 캐시
   * <p>
   * TTL: 1시간<br>
   * Key 패턴: ip-block::{ipAddress}
   * </p>
   *
   * <h4>용도:</h4>
   * <ul>
   *   <li>의심스러운 IP 임시 차단</li>
   *   <li>무차별 대입 공격 방어</li>
   * </ul>
   */
  public static final String IP_BLOCK = "ip-block";

  // ========================================
  // 캐시 키 생성 헬퍼
  // ========================================

  /**
   * 이메일 기반 AuthUser 캐시 키 생성
   *
   * @param email 이메일
   * @return 캐시 키
   */
  public static String authUserByEmail(String email) {
    return "email:" + email;
  }

  /**
   * userId 기반 AuthUser 캐시 키 생성
   *
   * @param userId User Service의 사용자 ID
   * @return 캐시 키
   */
  public static String authUserByUserId(String userId) {
    return "userId:" + userId;
  }

  /**
   * 토큰 값 기반 RefreshToken 캐시 키 생성
   *
   * @param token JWT 토큰 값
   * @return 캐시 키
   */
  public static String refreshTokenByToken(String token) {
    return "token:" + token;
  }

  /**
   * userId 기반 RefreshToken 목록 캐시 키 생성
   *
   * @param userId User Service의 사용자 ID
   * @return 캐시 키
   */
  public static String refreshTokensByUserId(String userId) {
    return "userId:" + userId;
  }

  /**
   * 로그인 시도 캐시 키 생성
   *
   * @param email 이메일
   * @return 캐시 키
   */
  public static String loginAttemptByEmail(String email) {
    return "email:" + email;
  }
}