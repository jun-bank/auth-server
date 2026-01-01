package com.jun_bank.auth_server.domain.auth.infrastructure.persistence.jpa;

import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AuthUser JPA Repository
 * <p>
 * Spring Data JPA 기본 CRUD 및 핵심 조회 메서드를 제공합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>PK/Unique Key 조회 제공</li>
 *   <li>존재 여부 확인 메서드 제공</li>
 *   <li>로그인 관련 특화 메서드 제공</li>
 * </ul>
 */
public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, String> {

  // ========================================
  // 단건 조회 (PK, Unique Key)
  // ========================================

  /**
   * 삭제되지 않은 인증 사용자 조회 (ID)
   *
   * @param authUserId 인증 사용자 ID
   * @return Optional<AuthUserEntity>
   */
  Optional<AuthUserEntity> findByAuthUserIdAndIsDeletedFalse(String authUserId);

  /**
   * 이메일로 인증 사용자 조회 (삭제되지 않은)
   * <p>
   * 로그인 시 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return Optional<AuthUserEntity>
   */
  Optional<AuthUserEntity> findByEmailAndIsDeletedFalse(String email);

  /**
   * User Service의 userId로 조회
   * <p>
   * User Service에서 사용자 정보 조회 시 사용합니다.
   * </p>
   *
   * @param userId User Service의 사용자 ID (USR-xxx)
   * @return Optional<AuthUserEntity>
   */
  Optional<AuthUserEntity> findByUserIdAndIsDeletedFalse(String userId);

  // ========================================
  // 존재 여부 확인
  // ========================================

  /**
   * 이메일 존재 여부 확인 (삭제되지 않은)
   * <p>
   * 회원가입 시 이메일 중복 체크에 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return 존재하면 true
   */
  boolean existsByEmailAndIsDeletedFalse(String email);

  /**
   * UserId 존재 여부 확인 (삭제되지 않은)
   *
   * @param userId User Service의 사용자 ID
   * @return 존재하면 true
   */
  boolean existsByUserIdAndIsDeletedFalse(String userId);

  // ========================================
  // 상태별 조회
  // ========================================

  /**
   * 특정 상태의 인증 사용자 목록 조회
   *
   * @param status 상태
   * @return List<AuthUserEntity>
   */
  List<AuthUserEntity> findByStatusAndIsDeletedFalse(AuthUserStatus status);

  /**
   * 잠금 해제 시간이 지난 LOCKED 상태 사용자 조회
   * <p>
   * 배치에서 자동 잠금 해제에 사용합니다.
   * </p>
   *
   * @param status 상태 (LOCKED)
   * @param now    현재 시간
   * @return List<AuthUserEntity>
   */
  @Query("SELECT a FROM AuthUserEntity a WHERE a.status = :status " +
      "AND a.lockedUntil < :now AND a.isDeleted = false")
  List<AuthUserEntity> findExpiredLockedUsers(
      @Param("status") AuthUserStatus status,
      @Param("now") LocalDateTime now);

  // ========================================
  // 배치 조회
  // ========================================

  /**
   * 여러 userId로 인증 사용자 목록 조회
   *
   * @param userIds User Service의 사용자 ID 목록
   * @return List<AuthUserEntity>
   */
  List<AuthUserEntity> findByUserIdInAndIsDeletedFalse(List<String> userIds);

  // ========================================
  // 배치 업데이트
  // ========================================

  /**
   * 만료된 잠금 상태를 일괄 해제
   * <p>
   * 배치에서 사용합니다.
   * </p>
   *
   * @param lockedStatus LOCKED 상태
   * @param activeStatus ACTIVE 상태
   * @param now          현재 시간
   * @return 업데이트된 행 수
   */
  @Modifying
  @Query("UPDATE AuthUserEntity a SET a.status = :activeStatus, a.failedLoginAttempts = 0, " +
      "a.lockedUntil = null WHERE a.status = :lockedStatus AND a.lockedUntil < :now")
  int unlockExpiredAccounts(
      @Param("lockedStatus") AuthUserStatus lockedStatus,
      @Param("activeStatus") AuthUserStatus activeStatus,
      @Param("now") LocalDateTime now);
}