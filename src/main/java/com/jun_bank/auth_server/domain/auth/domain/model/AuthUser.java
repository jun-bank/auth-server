package com.jun_bank.auth_server.domain.auth.domain.model;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.AuthUserId;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Email;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Password;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 인증 사용자 도메인 모델
 * <p>
 * 로그인 및 인증에 필요한 정보를 관리합니다.
 * User Service의 User와 userId(USR-xxx)로 연결됩니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>인증 정보 관리 (이메일, 암호화된 비밀번호)</li>
 *   <li>로그인 시도 관리 (실패 횟수, 계정 잠금)</li>
 *   <li>권한(Role) 관리</li>
 *   <li>인증 상태 관리</li>
 * </ul>
 *
 * <h3>User Service와의 관계:</h3>
 * <p>
 * 회원가입 시 User Service에서 Feign으로 Auth Server를 호출하여
 * 동기적으로 AuthUser를 생성합니다. userId 필드로 User와 연결됩니다.
 * </p>
 *
 * <h3>잠금 정책:</h3>
 * <ul>
 *   <li>기본 최대 시도 횟수: {@value #DEFAULT_MAX_ATTEMPTS}회</li>
 *   <li>기본 잠금 시간: {@value #DEFAULT_LOCK_MINUTES}분</li>
 *   <li>잠금 시간 경과 시 자동 해제 (로그인 시 확인)</li>
 * </ul>
 *
 * <h3>감사 필드 (BaseEntity 매핑):</h3>
 * <ul>
 *   <li>createdAt, updatedAt, createdBy, updatedBy</li>
 *   <li>deletedAt, deletedBy, isDeleted (Soft Delete)</li>
 * </ul>
 *
 * @see AuthUserStatus
 * @see UserRole
 * @see Password
 */
@Getter
public class AuthUser {

    /**
     * 기본 최대 로그인 실패 횟수
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 기본 잠금 시간 (분)
     */
    public static final int DEFAULT_LOCK_MINUTES = 30;

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 인증 사용자 ID
     * <p>Auth Server 내부 식별자 (AUT-xxx)</p>
     */
    private AuthUserId authUserId;

    /**
     * User Service의 UserId
     * <p>User 도메인과의 연결 키 (USR-xxx)</p>
     */
    private String userId;

    /**
     * 이메일 (로그인 ID)
     * <p>User Service의 이메일과 동일하게 유지됩니다.</p>
     */
    private Email email;

    /**
     * 암호화된 비밀번호
     * <p>BCrypt로 암호화된 값</p>
     */
    private Password password;

    /**
     * 사용자 역할 (권한)
     */
    private UserRole role;

    /**
     * 인증 상태
     */
    private AuthUserStatus status;

    /**
     * 로그인 실패 횟수
     * <p>성공 시 0으로 초기화됩니다.</p>
     */
    private int failedLoginAttempts;

    /**
     * 잠금 해제 시간
     * <p>이 시간이 지나면 자동으로 로그인 가능</p>
     */
    private LocalDateTime lockedUntil;

    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;

    // ========================================
    // 감사 필드 (BaseEntity 매핑)
    // ========================================

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;

    /** 생성자 ID */
    private String createdBy;

    /** 수정자 ID */
    private String updatedBy;

    /** 삭제 일시 (Soft Delete) */
    private LocalDateTime deletedAt;

    /** 삭제자 ID (Soft Delete) */
    private String deletedBy;

    /** 삭제 여부 (Soft Delete) */
    private Boolean isDeleted;

    /**
     * private 생성자
     */
    private AuthUser() {}

    // ========================================
    // 생성 메서드 (Builder 패턴)
    // ========================================

    /**
     * 신규 인증 사용자 생성 빌더
     * <p>
     * 회원가입 시 User Service에서 호출합니다.
     * status는 ACTIVE, role은 USER로 초기화됩니다.
     * </p>
     *
     * @return AuthUserCreateBuilder
     */
    public static AuthUserCreateBuilder createBuilder() {
        return new AuthUserCreateBuilder();
    }

    /**
     * DB 복원용 빌더
     *
     * @return AuthUserRestoreBuilder
     */
    public static AuthUserRestoreBuilder restoreBuilder() {
        return new AuthUserRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 여부 확인
     *
     * @return authUserId가 null이면 true
     */
    public boolean isNew() {
        return this.authUserId == null;
    }

    /**
     * 로그인 가능 여부 확인
     * <p>
     * DISABLED 상태는 로그인 불가.
     * LOCKED 상태는 잠금 시간 경과 여부에 따라 판단.
     * </p>
     *
     * @return 로그인 가능하면 true
     */
    public boolean canLogin() {
        if (this.status.isDisabled()) {
            return false;
        }
        if (this.status.isLocked()) {
            return isLockExpired();
        }
        return true;
    }

    /**
     * 현재 잠금 상태 여부 확인
     * <p>
     * LOCKED 상태이면서 잠금 시간이 남아있는 경우 true.
     * 잠금 시간이 경과한 경우 false.
     * </p>
     *
     * @return 실제로 잠겨있으면 true
     */
    public boolean isLocked() {
        if (!this.status.isLocked()) {
            return false;
        }
        return !isLockExpired();
    }

    /**
     * 잠금 시간 경과 여부 확인
     *
     * @return 잠금 시간이 지났으면 true
     */
    private boolean isLockExpired() {
        return this.lockedUntil != null && LocalDateTime.now().isAfter(this.lockedUntil);
    }

    /**
     * 남은 잠금 시간 반환 (분)
     *
     * @return 남은 시간 (분), 잠금 상태가 아니면 0
     */
    public long getRemainingLockMinutes() {
        if (!isLocked() || this.lockedUntil == null) {
            return 0;
        }
        Duration remaining = Duration.between(LocalDateTime.now(), this.lockedUntil);
        return Math.max(0, remaining.toMinutes());
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 비밀번호 변경
     * <p>
     * 새 비밀번호는 이미 암호화된 상태여야 합니다.
     * 평문 검증은 Application Layer에서 수행합니다.
     * </p>
     *
     * @param newPassword 새 비밀번호 (암호화된 상태)
     * @throws AuthException 비활성화 상태인 경우 (AUTH_021)
     */
    public void changePassword(Password newPassword) {
        if (this.status.isDisabled()) {
            throw AuthException.accountDisabled();
        }
        this.password = newPassword;
    }

    /**
     * 로그인 성공 처리
     * <p>
     * 실패 횟수 초기화, 마지막 로그인 시간 갱신.
     * LOCKED 상태였다면 ACTIVE로 변경.
     * </p>
     */
    public void recordLoginSuccess() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = LocalDateTime.now();

        if (this.status.isLocked()) {
            this.status = AuthUserStatus.ACTIVE;
            this.lockedUntil = null;
        }
    }

    /**
     * 로그인 실패 처리 (기본 정책 적용)
     * <p>
     * 실패 횟수 증가, 최대 횟수 초과 시 계정 잠금.
     * </p>
     *
     * @throws AuthException 이미 잠금/비활성화 상태인 경우
     */
    public void recordLoginFailure() {
        recordLoginFailure(DEFAULT_MAX_ATTEMPTS, DEFAULT_LOCK_MINUTES);
    }

    /**
     * 로그인 실패 처리 (커스텀 정책)
     *
     * @param maxAttempts 최대 시도 횟수
     * @param lockMinutes 잠금 시간 (분)
     * @throws AuthException 이미 잠금/비활성화 상태인 경우
     */
    public void recordLoginFailure(int maxAttempts, int lockMinutes) {
        if (this.status.isDisabled()) {
            throw AuthException.accountDisabled();
        }
        if (isLocked()) {
            throw AuthException.accountLocked(getRemainingLockMinutes());
        }

        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= maxAttempts) {
            this.status = AuthUserStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    /**
     * 수동 잠금 해제
     * <p>
     * 관리자에 의한 즉시 잠금 해제.
     * 이미 잠금 상태가 아니면 아무 작업도 하지 않습니다.
     * </p>
     */
    public void unlock() {
        if (!this.status.isLocked()) {
            return;
        }
        this.status = AuthUserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * 계정 비활성화
     * <p>
     * 관리자에 의한 영구 비활성화.
     * 이용약관 위반 등의 사유로 사용.
     * </p>
     */
    public void disable() {
        this.status = AuthUserStatus.DISABLED;
    }

    /**
     * 계정 활성화
     * <p>
     * 비활성화된 계정 재활성화.
     * 잠금 상태 초기화 포함.
     * </p>
     */
    public void enable() {
        if (this.status.isDisabled()) {
            this.status = AuthUserStatus.ACTIVE;
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
        }
    }

    /**
     * 역할(권한) 변경
     *
     * @param newRole 새 역할
     */
    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * 신규 인증 사용자 생성 빌더
     */
    public static class AuthUserCreateBuilder {
        private String userId;
        private Email email;
        private Password password;
        private UserRole role = UserRole.USER;

        /**
         * User Service의 UserId 설정
         *
         * @param userId USR-xxx 형식
         * @return this
         */
        public AuthUserCreateBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 이메일 설정
         *
         * @param email 이메일 VO
         * @return this
         */
        public AuthUserCreateBuilder email(Email email) {
            this.email = email;
            return this;
        }

        /**
         * 비밀번호 설정
         *
         * @param password 암호화된 비밀번호 VO
         * @return this
         */
        public AuthUserCreateBuilder password(Password password) {
            this.password = password;
            return this;
        }

        /**
         * 역할 설정 (기본값: USER)
         *
         * @param role 사용자 역할
         * @return this
         */
        public AuthUserCreateBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        /**
         * AuthUser 객체 생성
         *
         * @return 신규 AuthUser 객체
         * @throws AuthException 필수 필드 누락
         */
        public AuthUser build() {
            AuthUser authUser = new AuthUser();
            authUser.userId = this.userId;
            authUser.email = this.email;
            authUser.password = this.password;
            authUser.role = this.role;
            authUser.status = AuthUserStatus.ACTIVE;
            authUser.failedLoginAttempts = 0;
            authUser.isDeleted = false;

            if (authUser.email == null) {
                throw AuthException.invalidEmailFormat(null);
            }
            if (authUser.password == null) {
                throw AuthException.invalidPasswordFormat();
            }

            return authUser;
        }
    }

    /**
     * DB 복원용 빌더
     */
    public static class AuthUserRestoreBuilder {
        private AuthUserId authUserId;
        private String userId;
        private Email email;
        private Password password;
        private UserRole role;
        private AuthUserStatus status;
        private int failedLoginAttempts;
        private LocalDateTime lockedUntil;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        public AuthUserRestoreBuilder authUserId(AuthUserId authUserId) {
            this.authUserId = authUserId;
            return this;
        }

        public AuthUserRestoreBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public AuthUserRestoreBuilder email(Email email) {
            this.email = email;
            return this;
        }

        public AuthUserRestoreBuilder password(Password password) {
            this.password = password;
            return this;
        }

        public AuthUserRestoreBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        public AuthUserRestoreBuilder status(AuthUserStatus status) {
            this.status = status;
            return this;
        }

        public AuthUserRestoreBuilder failedLoginAttempts(int failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public AuthUserRestoreBuilder lockedUntil(LocalDateTime lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public AuthUserRestoreBuilder lastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public AuthUserRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuthUserRestoreBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AuthUserRestoreBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public AuthUserRestoreBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public AuthUserRestoreBuilder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public AuthUserRestoreBuilder deletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
            return this;
        }

        public AuthUserRestoreBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public AuthUser build() {
            AuthUser authUser = new AuthUser();
            authUser.authUserId = this.authUserId;
            authUser.userId = this.userId;
            authUser.email = this.email;
            authUser.password = this.password;
            authUser.role = this.role;
            authUser.status = this.status;
            authUser.failedLoginAttempts = this.failedLoginAttempts;
            authUser.lockedUntil = this.lockedUntil;
            authUser.lastLoginAt = this.lastLoginAt;
            authUser.createdAt = this.createdAt;
            authUser.updatedAt = this.updatedAt;
            authUser.createdBy = this.createdBy;
            authUser.updatedBy = this.updatedBy;
            authUser.deletedAt = this.deletedAt;
            authUser.deletedBy = this.deletedBy;
            authUser.isDeleted = this.isDeleted;
            return authUser;
        }
    }
}