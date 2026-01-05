package com.jun_bank.auth_server.domain.auth.application.port.out;

import com.jun_bank.auth_server.domain.auth.domain.model.AuthUser;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;

import java.util.List;
import java.util.Optional;

/**
 * AuthUser Repository Port (Output Port)
 * <p>
 * Application Layer에서 사용하는 Repository 인터페이스입니다.
 * Infrastructure Layer의 구현체와 분리하여 의존성 역전을 실현합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>도메인 모델(AuthUser)만 다룸 - Entity 노출 안함</li>
 *   <li>Infrastructure 의존성 없음</li>
 *   <li>비즈니스 관점의 메서드명 사용</li>
 * </ul>
 *
 * <h3>메서드 분류:</h3>
 * <ul>
 *   <li>핵심 조회: ID, 이메일, userId로 단건 조회</li>
 *   <li>존재 여부: 중복 체크</li>
 *   <li>저장/삭제: 명시적 메서드</li>
 *   <li>배치: 상태별 조회, 잠금 해제</li>
 * </ul>
 */
public interface AuthUserRepository {

  // ========================================
  // 저장
  // ========================================

  /**
   * 인증 사용자 저장 (신규 생성 또는 수정)
   * <p>
   * {@link AuthUser#isNew()}가 true이면 신규 저장 (ID 자동 생성),
   * false이면 기존 엔티티 수정 (더티체킹)
   * </p>
   *
   * @param authUser 저장할 인증 사용자
   * @return 저장된 인증 사용자 (ID 포함)
   */
  AuthUser save(AuthUser authUser);

  // ========================================
  // 단건 조회
  // ========================================

  /**
   * ID로 인증 사용자 조회
   *
   * @param authUserId 인증 사용자 ID (AUT-xxx)
   * @return Optional<AuthUser>
   */
  Optional<AuthUser> findById(String authUserId);

  /**
   * 이메일로 인증 사용자 조회
   * <p>
   * 로그인 시 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return Optional<AuthUser>
   */
  Optional<AuthUser> findByEmail(String email);

  /**
   * User Service의 userId로 인증 사용자 조회
   * <p>
   * User Service에서 인증 정보 조회 시 사용합니다.
   * </p>
   *
   * @param userId User Service의 사용자 ID (USR-xxx)
   * @return Optional<AuthUser>
   */
  Optional<AuthUser> findByUserId(String userId);

  // ========================================
  // 존재 여부 확인
  // ========================================

  /**
   * 이메일 존재 여부 확인
   * <p>
   * 회원가입 시 이메일 중복 체크에 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return 존재하면 true
   */
  boolean existsByEmail(String email);

  /**
   * userId 존재 여부 확인
   *
   * @param userId User Service의 사용자 ID
   * @return 존재하면 true
   */
  boolean existsByUserId(String userId);

  // ========================================
  // 상태별 조회
  // ========================================

  /**
   * 특정 상태의 인증 사용자 목록 조회
   *
   * @param status 상태
   * @return List<AuthUser>
   */
  List<AuthUser> findByStatus(AuthUserStatus status);

  /**
   * 잠금 시간이 만료된 LOCKED 상태 사용자 조회
   * <p>
   * 배치에서 자동 잠금 해제에 사용합니다.
   * </p>
   *
   * @return 잠금 해제 대상 사용자 목록
   */
  List<AuthUser> findExpiredLockedUsers();

  // ========================================
  // 배치 조회
  // ========================================

  /**
   * 여러 userId로 인증 사용자 목록 조회
   *
   * @param userIds User Service의 사용자 ID 목록
   * @return List<AuthUser>
   */
  List<AuthUser> findByUserIds(List<String> userIds);

  // ========================================
  // 배치 업데이트
  // ========================================

  /**
   * 만료된 잠금 상태를 일괄 해제
   * <p>
   * 배치에서 사용합니다.
   * </p>
   *
   * @return 업데이트된 행 수
   */
  int unlockExpiredAccounts();

  // ========================================
  // 삭제
  // ========================================

  /**
   * 인증 사용자 삭제 (Soft Delete)
   * <p>
   * 회원 탈퇴 시 User Service에서 호출합니다.
   * 실제 삭제가 아닌 상태 변경으로 처리합니다.
   * </p>
   *
   * @param userId    삭제할 User Service의 사용자 ID
   * @param deletedBy 삭제자 ID
   */
  void deleteByUserId(String userId, String deletedBy);

  // ========================================
  // 로그인 시도 관리 (Cache)
  // ========================================

  /**
   * 로그인 실패 기록
   * <p>
   * Redis에 원자적으로 실패 횟수를 증가시킵니다.
   * 최대 횟수 초과 시 자동으로 계정이 잠깁니다.
   * </p>
   *
   * @param email 이메일
   * @return AttemptResult (상태, 시도 횟수, 남은 잠금 시간)
   */
  LoginAttemptResult recordLoginFailure(String email);

  /**
   * 로그인 성공 기록
   * <p>
   * Redis의 실패 카운터를 초기화합니다.
   * </p>
   *
   * @param email 이메일
   */
  void recordLoginSuccess(String email);

  /**
   * 계정 잠금 여부 확인
   * <p>
   * Redis에서 빠르게 확인합니다.
   * </p>
   *
   * @param email 이메일
   * @return 잠금 상태면 true
   */
  boolean isAccountLocked(String email);

  /**
   * 현재 실패 횟수 조회
   *
   * @param email 이메일
   * @return 실패 횟수
   */
  int getFailedAttempts(String email);

  /**
   * 남은 잠금 시간 조회 (초)
   *
   * @param email 이메일
   * @return 남은 시간 (초), 잠금 상태가 아니면 0
   */
  long getRemainingLockSeconds(String email);

  // ========================================
  // 로그인 시도 결과 DTO
  // ========================================

  /**
   * 로그인 시도 상태
   */
  enum LoginAttemptStatus {
    /** 정상 */
    OK,
    /** 이번 요청으로 잠금됨 */
    LOCKED,
    /** 이미 잠금 상태 */
    ALREADY_LOCKED,
    /** 에러 발생 */
    ERROR;

    public boolean isLocked() {
      return this == LOCKED || this == ALREADY_LOCKED;
    }
  }

  /**
   * 로그인 시도 결과
   */
  record LoginAttemptResult(
          String email,
          LoginAttemptStatus status,
          int attempts,
          long remainingSeconds,
          String errorMessage
  ) {
    public boolean isLocked() {
      return status.isLocked();
    }

    public boolean isSuccess() {
      return status != LoginAttemptStatus.ERROR;
    }

    public boolean justLocked() {
      return status == LoginAttemptStatus.LOCKED;
    }

    public long getRemainingMinutes() {
      return remainingSeconds / 60;
    }

    public static LoginAttemptResult error(String email, String message) {
      return new LoginAttemptResult(email, LoginAttemptStatus.ERROR, 0, 0, message);
    }
  }
}