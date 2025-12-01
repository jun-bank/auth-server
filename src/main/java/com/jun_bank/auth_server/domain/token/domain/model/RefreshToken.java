package com.jun_bank.auth_server.domain.token.domain.model;

import com.jun_bank.auth_server.domain.token.domain.exception.TokenException;
import com.jun_bank.auth_server.domain.token.domain.model.vo.RefreshTokenId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 도메인 모델
 * <p>
 * JWT 리프레시 토큰 정보를 관리합니다.
 * Access Token 갱신에 사용됩니다.
 *
 * <h3>토큰 라이프사이클:</h3>
 * <ol>
 *   <li><b>생성</b>: 로그인 성공 시</li>
 *   <li><b>사용</b>: Access Token 갱신 요청 시</li>
 *   <li><b>폐기</b>: 로그아웃, 만료, 보안 이슈 시</li>
 * </ol>
 *
 * <h3>보안 정책:</h3>
 * <ul>
 *   <li>디바이스/IP 기반 검증 지원</li>
 *   <li>단일 사용 후 재발급 (Rotation) 가능</li>
 *   <li>명시적 폐기(revoke) 지원</li>
 * </ul>
 *
 * <h3>감사 필드:</h3>
 * <p>
 * RefreshToken은 createdAt만 사용합니다.
 * 생성 후 수정되지 않고, 폐기 시 isRevoked 플래그만 변경됩니다.
 * </p>
 *
 * @see RefreshTokenId
 */
@Getter
public class RefreshToken {

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 리프레시 토큰 ID (DB 식별자)
     */
    private RefreshTokenId refreshTokenId;

    /**
     * User Service의 UserId
     * <p>토큰 소유자 식별</p>
     */
    private String userId;

    /**
     * 실제 토큰 값 (JWT)
     */
    private String token;

    /**
     * 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * 폐기 여부
     * <p>로그아웃 등으로 명시적 폐기 시 true</p>
     */
    private boolean isRevoked;

    /**
     * 디바이스 정보
     * <p>User-Agent 등</p>
     */
    private String deviceInfo;

    /**
     * 접속 IP
     */
    private String ipAddress;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * private 생성자
     */
    private RefreshToken() {}

    // ========================================
    // 생성 메서드
    // ========================================

    /**
     * 신규 리프레시 토큰 생성 빌더
     * <p>
     * 로그인 성공 시 사용합니다.
     * isRevoked는 false로 초기화됩니다.
     * </p>
     *
     * @return RefreshTokenCreateBuilder
     */
    public static RefreshTokenCreateBuilder createBuilder() {
        return new RefreshTokenCreateBuilder();
    }

    /**
     * DB 복원용 빌더
     *
     * @return RefreshTokenRestoreBuilder
     */
    public static RefreshTokenRestoreBuilder restoreBuilder() {
        return new RefreshTokenRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 여부 확인
     *
     * @return refreshTokenId가 null이면 true
     */
    public boolean isNew() {
        return this.refreshTokenId == null;
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @return 현재 시간이 만료 시간을 지났으면 true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰 유효성 확인
     * <p>
     * 폐기되지 않고 만료되지 않은 경우에만 유효합니다.
     * </p>
     *
     * @return 유효하면 true
     */
    public boolean isValid() {
        return !this.isRevoked && !isExpired();
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 토큰 사용 전 유효성 검증
     * <p>
     * Access Token 갱신 요청 시 호출합니다.
     * 유효하지 않으면 적절한 예외를 발생시킵니다.
     * </p>
     *
     * @throws TokenException 폐기된 토큰인 경우 (TKN_003)
     * @throws TokenException 만료된 토큰인 경우 (TKN_002)
     */
    public void validateForUse() {
        if (this.isRevoked) {
            throw TokenException.tokenRevoked();
        }
        if (isExpired()) {
            throw TokenException.tokenExpired();
        }
    }

    /**
     * 토큰 폐기
     * <p>
     * 로그아웃 또는 보안 상의 이유로 토큰을 폐기합니다.
     * 이미 폐기된 토큰에 대해서는 아무 작업도 하지 않습니다 (멱등성).
     * </p>
     */
    public void revoke() {
        this.isRevoked = true;
    }

    /**
     * 디바이스/IP 일치 여부 확인
     * <p>
     * 토큰이 발급된 환경과 현재 요청 환경이 일치하는지 확인합니다.
     * 다른 환경에서의 토큰 사용을 감지하는 데 활용할 수 있습니다.
     * null인 경우 해당 필드는 검증을 건너뜁니다.
     * </p>
     *
     * @param deviceInfo 요청 디바이스 정보
     * @param ipAddress 요청 IP
     * @return 일치하면 true
     */
    public boolean matchesContext(String deviceInfo, String ipAddress) {
        boolean deviceMatch = this.deviceInfo == null || this.deviceInfo.equals(deviceInfo);
        boolean ipMatch = this.ipAddress == null || this.ipAddress.equals(ipAddress);
        return deviceMatch && ipMatch;
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * 신규 리프레시 토큰 생성 빌더
     */
    public static class RefreshTokenCreateBuilder {
        private String userId;
        private String token;
        private LocalDateTime expiresAt;
        private String deviceInfo;
        private String ipAddress;

        public RefreshTokenCreateBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RefreshTokenCreateBuilder token(String token) {
            this.token = token;
            return this;
        }

        public RefreshTokenCreateBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public RefreshTokenCreateBuilder deviceInfo(String deviceInfo) {
            this.deviceInfo = deviceInfo;
            return this;
        }

        public RefreshTokenCreateBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public RefreshToken build() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.userId = this.userId;
            refreshToken.token = this.token;
            refreshToken.expiresAt = this.expiresAt;
            refreshToken.isRevoked = false;
            refreshToken.deviceInfo = this.deviceInfo;
            refreshToken.ipAddress = this.ipAddress;

            if (refreshToken.token == null || refreshToken.token.isBlank()) {
                throw TokenException.invalidToken();
            }

            return refreshToken;
        }
    }

    /**
     * DB 복원용 빌더
     */
    public static class RefreshTokenRestoreBuilder {
        private RefreshTokenId refreshTokenId;
        private String userId;
        private String token;
        private LocalDateTime expiresAt;
        private boolean isRevoked;
        private String deviceInfo;
        private String ipAddress;
        private LocalDateTime createdAt;

        public RefreshTokenRestoreBuilder refreshTokenId(RefreshTokenId refreshTokenId) {
            this.refreshTokenId = refreshTokenId;
            return this;
        }

        public RefreshTokenRestoreBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RefreshTokenRestoreBuilder token(String token) {
            this.token = token;
            return this;
        }

        public RefreshTokenRestoreBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public RefreshTokenRestoreBuilder isRevoked(boolean isRevoked) {
            this.isRevoked = isRevoked;
            return this;
        }

        public RefreshTokenRestoreBuilder deviceInfo(String deviceInfo) {
            this.deviceInfo = deviceInfo;
            return this;
        }

        public RefreshTokenRestoreBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public RefreshTokenRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RefreshToken build() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.refreshTokenId = this.refreshTokenId;
            refreshToken.userId = this.userId;
            refreshToken.token = this.token;
            refreshToken.expiresAt = this.expiresAt;
            refreshToken.isRevoked = this.isRevoked;
            refreshToken.deviceInfo = this.deviceInfo;
            refreshToken.ipAddress = this.ipAddress;
            refreshToken.createdAt = this.createdAt;
            return refreshToken;
        }
    }
}