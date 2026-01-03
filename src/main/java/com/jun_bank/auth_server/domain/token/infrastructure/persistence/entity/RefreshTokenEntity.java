package com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 JPA 엔티티
 * <p>
 * 도메인 모델 {@link com.jun_bank.auth_server.domain.token.domain.model.RefreshToken}과
 * DB 테이블을 매핑합니다.
 *
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: refresh_tokens</li>
 *   <li>스키마: auth_db</li>
 * </ul>
 *
 * <h3>인덱스:</h3>
 * <ul>
 *   <li>idx_refresh_token_user_id: user_id</li>
 *   <li>idx_refresh_token_token: token (유니크)</li>
 *   <li>idx_refresh_token_expires_at: expires_at</li>
 * </ul>
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li>BaseEntity 미상속 - createdAt만 사용</li>
 *   <li>수정 없이 생성 후 폐기만 가능</li>
 *   <li>Soft Delete 미적용 - 물리 삭제 또는 is_revoked 사용</li>
 * </ul>
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_token_token", columnList = "token", unique = true),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshTokenEntity {

  /**
   * 리프레시 토큰 ID (PK)
   * <p>RTK-xxxxxxxx 형식</p>
   */
  @Id
  @Column(name = "refresh_token_id", length = 12, nullable = false)
  private String refreshTokenId;

  /**
   * User Service의 UserId
   * <p>토큰 소유자 식별</p>
   */
  @Column(name = "user_id", length = 12, nullable = false)
  private String userId;

  /**
   * 실제 토큰 값 (JWT)
   */
  @Column(name = "token", length = 500, nullable = false, unique = true)
  private String token;

  /**
   * 만료 시간
   */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  /**
   * 폐기 여부
   * <p>로그아웃 등으로 명시적 폐기 시 true</p>
   */
  @Column(name = "is_revoked", nullable = false)
  private boolean isRevoked;

  /**
   * 디바이스 정보
   * <p>User-Agent 등</p>
   */
  @Column(name = "device_info", length = 500)
  private String deviceInfo;

  /**
   * 접속 IP
   */
  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  /**
   * 생성 시간
   */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  // ========================================
  // 생성자 및 팩토리 메서드
  // ========================================

  private RefreshTokenEntity(String refreshTokenId, String userId, String token,
                             LocalDateTime expiresAt, boolean isRevoked,
                             String deviceInfo, String ipAddress) {
    this.refreshTokenId = refreshTokenId;
    this.userId = userId;
    this.token = token;
    this.expiresAt = expiresAt;
    this.isRevoked = isRevoked;
    this.deviceInfo = deviceInfo;
    this.ipAddress = ipAddress;
  }

  /**
   * 신규 엔티티 생성용 정적 팩토리 메서드
   *
   * @param refreshTokenId 리프레시 토큰 ID
   * @param userId         User Service의 사용자 ID
   * @param token          JWT 토큰 값
   * @param expiresAt      만료 시간
   * @param deviceInfo     디바이스 정보
   * @param ipAddress      접속 IP
   * @return RefreshTokenEntity
   */
  public static RefreshTokenEntity of(String refreshTokenId, String userId, String token,
                                      LocalDateTime expiresAt, String deviceInfo,
                                      String ipAddress) {
    return new RefreshTokenEntity(refreshTokenId, userId, token, expiresAt,
            false, deviceInfo, ipAddress);
  }

  // ========================================
  // 상태 변경 메서드
  // ========================================

  /**
   * 토큰 폐기
   * <p>
   * 로그아웃 또는 보안 상의 이유로 토큰을 폐기합니다.
   * </p>
   */
  public void revoke() {
    this.isRevoked = true;
  }

  /**
   * 만료 여부 확인
   *
   * @return 현재 시간이 만료 시간을 지났으면 true
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }

  /**
   * 유효 여부 확인
   *
   * @return 폐기되지 않고 만료되지 않았으면 true
   */
  public boolean isValid() {
    return !this.isRevoked && !isExpired();
  }
}