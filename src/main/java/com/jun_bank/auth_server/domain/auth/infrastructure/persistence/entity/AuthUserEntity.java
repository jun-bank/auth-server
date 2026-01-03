package com.jun_bank.auth_server.domain.auth.infrastructure.persistence.entity;

import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;
import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import com.jun_bank.auth_server.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증 사용자 JPA 엔티티
 * <p>
 * 도메인 모델 {@link com.jun_bank.auth_server.domain.auth.domain.model.AuthUser}와
 * DB 테이블을 매핑합니다.
 *
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: auth_users</li>
 *   <li>스키마: auth_db</li>
 * </ul>
 *
 * <h3>인덱스:</h3>
 * <ul>
 *   <li>idx_auth_user_email: email (유니크)</li>
 *   <li>idx_auth_user_user_id: user_id (User Service 연결)</li>
 *   <li>idx_auth_user_status: status</li>
 *   <li>idx_auth_user_is_deleted: is_deleted</li>
 * </ul>
 *
 * @see BaseEntity
 */
@Entity
@Table(
        name = "auth_users",
        indexes = {
                @Index(name = "idx_auth_user_email", columnList = "email", unique = true),
                @Index(name = "idx_auth_user_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_user_status", columnList = "status"),
                @Index(name = "idx_auth_user_is_deleted", columnList = "is_deleted")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthUserEntity extends BaseEntity {

  /**
   * 인증 사용자 ID (PK)
   * <p>AUT-xxxxxxxx 형식</p>
   */
  @Id
  @Column(name = "auth_user_id", length = 12, nullable = false)
  private String authUserId;

  /**
   * User Service의 UserId
   * <p>USR-xxxxxxxx 형식, User 도메인과의 연결 키</p>
   */
  @Column(name = "user_id", length = 12, nullable = false)
  private String userId;

  /**
   * 이메일 (로그인 ID, 유니크)
   */
  @Column(name = "email", length = 255, nullable = false, unique = true)
  private String email;

  /**
   * 암호화된 비밀번호
   * <p>BCrypt 해시값</p>
   */
  @Column(name = "password", length = 255, nullable = false)
  private String password;

  /**
   * 사용자 역할 (권한)
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 20, nullable = false)
  private UserRole role;

  /**
   * 인증 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private AuthUserStatus status;

  /**
   * 로그인 실패 횟수
   */
  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  /**
   * 잠금 해제 시간
   */
  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  /**
   * 마지막 로그인 시간
   */
  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // ========================================
  // 생성자 및 팩토리 메서드
  // ========================================

  /**
   * 전체 필드 생성자 (Mapper에서 사용)
   */
  private AuthUserEntity(String authUserId, String userId, String email, String password,
                         UserRole role, AuthUserStatus status, int failedLoginAttempts,
                         LocalDateTime lockedUntil, LocalDateTime lastLoginAt) {
    this.authUserId = authUserId;
    this.userId = userId;
    this.email = email;
    this.password = password;
    this.role = role;
    this.status = status;
    this.failedLoginAttempts = failedLoginAttempts;
    this.lockedUntil = lockedUntil;
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * 신규 엔티티 생성용 정적 팩토리 메서드
   *
   * @param authUserId          인증 사용자 ID
   * @param userId              User Service의 사용자 ID
   * @param email               이메일
   * @param password            암호화된 비밀번호
   * @param role                역할
   * @param status              상태
   * @param failedLoginAttempts 로그인 실패 횟수
   * @param lockedUntil         잠금 해제 시간
   * @param lastLoginAt         마지막 로그인 시간
   * @return AuthUserEntity
   */
  public static AuthUserEntity of(String authUserId, String userId, String email,
                                  String password, UserRole role, AuthUserStatus status,
                                  int failedLoginAttempts, LocalDateTime lockedUntil,
                                  LocalDateTime lastLoginAt) {
    return new AuthUserEntity(authUserId, userId, email, password, role, status,
            failedLoginAttempts, lockedUntil, lastLoginAt);
  }

  // ========================================
  // 업데이트 메서드
  // ========================================

  /**
   * 인증 정보 업데이트
   * <p>
   * 변경 가능한 필드만 업데이트합니다.
   * 불변 필드(authUserId, userId, email)는 변경하지 않습니다.
   * </p>
   *
   * @param password            새 비밀번호 (암호화된 상태)
   * @param role                새 역할
   * @param status              새 상태
   * @param failedLoginAttempts 로그인 실패 횟수
   * @param lockedUntil         잠금 해제 시간
   * @param lastLoginAt         마지막 로그인 시간
   */
  public void update(String password, UserRole role, AuthUserStatus status,
                     int failedLoginAttempts, LocalDateTime lockedUntil,
                     LocalDateTime lastLoginAt) {
    this.password = password;
    this.role = role;
    this.status = status;
    this.failedLoginAttempts = failedLoginAttempts;
    this.lockedUntil = lockedUntil;
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * 상태만 업데이트
   *
   * @param status 새 상태
   */
  public void updateStatus(AuthUserStatus status) {
    this.status = status;
  }

  /**
   * 비밀번호만 업데이트
   *
   * @param password 새 비밀번호 (암호화된 상태)
   */
  public void updatePassword(String password) {
    this.password = password;
  }

  /**
   * 로그인 성공 기록
   */
  public void recordLoginSuccess() {
    this.failedLoginAttempts = 0;
    this.lastLoginAt = LocalDateTime.now();
    if (this.status == AuthUserStatus.LOCKED) {
      this.status = AuthUserStatus.ACTIVE;
      this.lockedUntil = null;
    }
  }

  /**
   * 로그인 실패 기록
   *
   * @param maxAttempts 최대 시도 횟수
   * @param lockMinutes 잠금 시간 (분)
   */
  public void recordLoginFailure(int maxAttempts, int lockMinutes) {
    this.failedLoginAttempts++;
    if (this.failedLoginAttempts >= maxAttempts) {
      this.status = AuthUserStatus.LOCKED;
      this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
    }
  }
}