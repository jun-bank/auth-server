package com.jun_bank.auth_server.domain.history.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 로그인 이력 JPA 엔티티
 * <p>
 * 도메인 모델 {@link com.jun_bank.auth_server.domain.history.domain.model.LoginHistory}와
 * DB 테이블을 매핑합니다.
 *
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: login_histories</li>
 *   <li>스키마: auth_db</li>
 * </ul>
 *
 * <h3>인덱스:</h3>
 * <ul>
 *   <li>idx_login_history_user_id: user_id</li>
 *   <li>idx_login_history_email: email</li>
 *   <li>idx_login_history_login_at: login_at</li>
 *   <li>idx_login_history_success: success</li>
 *   <li>idx_login_history_ip: ip_address</li>
 * </ul>
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li><b>Append-only</b>: INSERT만 허용, UPDATE/DELETE 금지</li>
 *   <li>BaseEntity 미상속 - 감사/Soft Delete 불필요</li>
 *   <li>법적 보관 의무로 인해 삭제 불가 (일정 기간 후 배치로 정리)</li>
 * </ul>
 */
@Entity
@Table(
        name = "login_histories",
        indexes = {
                @Index(name = "idx_login_history_user_id", columnList = "user_id"),
                @Index(name = "idx_login_history_email", columnList = "email"),
                @Index(name = "idx_login_history_login_at", columnList = "login_at"),
                @Index(name = "idx_login_history_success", columnList = "success"),
                @Index(name = "idx_login_history_ip", columnList = "ip_address")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistoryEntity {

    /**
     * 로그인 이력 ID (PK)
     * <p>LGH-xxxxxxxx 형식</p>
     */
    @Id
    @Column(name = "login_history_id", length = 12, nullable = false)
    private String loginHistoryId;

    /**
     * User Service의 UserId
     * <p>
     * 로그인 실패 시 사용자를 찾을 수 없는 경우 null일 수 있습니다.
     * </p>
     */
    @Column(name = "user_id", length = 12)
    private String userId;

    /**
     * 로그인 시도 이메일
     * <p>
     * 사용자가 존재하지 않더라도 입력된 이메일을 기록합니다.
     * </p>
     */
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    /**
     * 로그인 시도 시간
     */
    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    /**
     * 접속 IP 주소
     */
    @Column(name = "ip_address", length = 50, nullable = false)
    private String ipAddress;

    /**
     * User-Agent
     * <p>브라우저/앱 정보</p>
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 성공 여부
     */
    @Column(name = "success", nullable = false)
    private boolean success;

    /**
     * 실패 사유
     * <p>
     * 성공 시 null, 실패 시 구체적인 사유 기록.
     * 예: "INVALID_PASSWORD", "ACCOUNT_LOCKED", "USER_NOT_FOUND"
     * </p>
     */
    @Column(name = "fail_reason", length = 100)
    private String failReason;

    // ========================================
    // 생성자 및 팩토리 메서드
    // ========================================

    private LoginHistoryEntity(String loginHistoryId, String userId, String email,
                               LocalDateTime loginAt, String ipAddress, String userAgent,
                               boolean success, String failReason) {
        this.loginHistoryId = loginHistoryId;
        this.userId = userId;
        this.email = email;
        this.loginAt = loginAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failReason = failReason;
    }

    /**
     * 로그인 성공 이력 엔티티 생성
     *
     * @param loginHistoryId 로그인 이력 ID
     * @param userId         User Service의 사용자 ID
     * @param email          로그인한 이메일
     * @param loginAt        로그인 시간
     * @param ipAddress      접속 IP
     * @param userAgent      User-Agent
     * @return LoginHistoryEntity
     */
    public static LoginHistoryEntity success(String loginHistoryId, String userId, String email,
                                             LocalDateTime loginAt, String ipAddress,
                                             String userAgent) {
        return new LoginHistoryEntity(loginHistoryId, userId, email, loginAt,
                ipAddress, userAgent, true, null);
    }

    /**
     * 로그인 실패 이력 엔티티 생성
     *
     * @param loginHistoryId 로그인 이력 ID
     * @param userId         User Service의 사용자 ID (없을 수 있음)
     * @param email          로그인 시도한 이메일
     * @param loginAt        로그인 시도 시간
     * @param ipAddress      접속 IP
     * @param userAgent      User-Agent
     * @param failReason     실패 사유
     * @return LoginHistoryEntity
     */
    public static LoginHistoryEntity failure(String loginHistoryId, String userId, String email,
                                             LocalDateTime loginAt, String ipAddress,
                                             String userAgent, String failReason) {
        return new LoginHistoryEntity(loginHistoryId, userId, email, loginAt,
                ipAddress, userAgent, false, failReason);
    }

    // ========================================
    // Append-only: UPDATE/DELETE 메서드 없음
    // ========================================
    // 이 엔티티는 INSERT만 허용됩니다.
    // 수정/삭제 메서드를 의도적으로 제공하지 않습니다.
}