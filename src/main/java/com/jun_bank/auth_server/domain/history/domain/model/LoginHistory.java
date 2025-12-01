package com.jun_bank.auth_server.domain.history.domain.model;

import com.jun_bank.auth_server.domain.history.domain.model.vo.LoginHistoryId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 로그인 이력 도메인 모델
 * <p>
 * 로그인 시도 기록을 저장합니다.
 * <strong>Append-only 테이블:</strong> INSERT만 허용, UPDATE/DELETE 금지.
 *
 * <h3>기록 내용:</h3>
 * <ul>
 *   <li>로그인 시도 시간</li>
 *   <li>성공/실패 여부</li>
 *   <li>실패 사유 (실패 시)</li>
 *   <li>접속 정보 (IP, User-Agent)</li>
 * </ul>
 *
 * <h3>활용 목적:</h3>
 * <ul>
 *   <li><b>보안 감사 (Audit)</b>: 로그인 시도 추적</li>
 *   <li><b>이상 징후 탐지</b>: 비정상적인 로그인 패턴 감지</li>
 *   <li><b>사용자 지원</b>: 접속 기록 조회</li>
 *   <li><b>컴플라이언스</b>: 금융 규정 준수를 위한 로그 보관</li>
 * </ul>
 *
 * <h3>Append-only 특성:</h3>
 * <ul>
 *   <li>생성 후 수정 메서드 없음</li>
 *   <li>삭제 기능 없음 (법적 보관 의무)</li>
 *   <li>팩토리 메서드로만 생성</li>
 * </ul>
 *
 * @see LoginHistoryId
 */
@Getter
public class LoginHistory {

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 로그인 이력 ID
     */
    private LoginHistoryId loginHistoryId;

    /**
     * User Service의 UserId
     * <p>
     * 로그인 실패 시 사용자를 찾을 수 없는 경우 null일 수 있습니다.
     * </p>
     */
    private String userId;

    /**
     * 로그인 시도 이메일
     * <p>
     * 사용자가 존재하지 않더라도 입력된 이메일을 기록합니다.
     * </p>
     */
    private String email;

    /**
     * 로그인 시도 시간
     */
    private LocalDateTime loginAt;

    /**
     * 접속 IP 주소
     */
    private String ipAddress;

    /**
     * User-Agent
     * <p>브라우저/앱 정보</p>
     */
    private String userAgent;

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 실패 사유
     * <p>
     * 성공 시 null, 실패 시 구체적인 사유 기록.
     * 예: "INVALID_PASSWORD", "ACCOUNT_LOCKED", "USER_NOT_FOUND"
     * </p>
     */
    private String failReason;

    /**
     * private 생성자
     * <p>팩토리 메서드를 통해서만 인스턴스를 생성합니다.</p>
     */
    private LoginHistory() {}

    // ========================================
    // 팩토리 메서드
    // ========================================

    /**
     * 로그인 성공 이력 생성
     * <p>
     * 로그인이 성공한 경우 호출합니다.
     * success는 true, failReason은 null로 설정됩니다.
     * </p>
     *
     * @param userId User Service의 사용자 ID (USR-xxx)
     * @param email 로그인한 이메일
     * @param ipAddress 접속 IP
     * @param userAgent User-Agent 문자열
     * @return 성공 로그인 이력
     */
    public static LoginHistory success(String userId, String email,
                                       String ipAddress, String userAgent) {
        LoginHistory history = new LoginHistory();
        history.userId = userId;
        history.email = email;
        history.loginAt = LocalDateTime.now();
        history.ipAddress = ipAddress;
        history.userAgent = userAgent;
        history.success = true;
        history.failReason = null;
        return history;
    }

    /**
     * 로그인 실패 이력 생성
     * <p>
     * 로그인이 실패한 경우 호출합니다.
     * success는 false, failReason에 실패 사유가 기록됩니다.
     * </p>
     *
     * <h4>실패 사유 예시:</h4>
     * <ul>
     *   <li>"INVALID_PASSWORD": 비밀번호 불일치</li>
     *   <li>"USER_NOT_FOUND": 존재하지 않는 이메일</li>
     *   <li>"ACCOUNT_LOCKED": 계정 잠금</li>
     *   <li>"ACCOUNT_DISABLED": 계정 비활성화</li>
     * </ul>
     *
     * @param userId User Service의 사용자 ID (없을 수 있음)
     * @param email 로그인 시도한 이메일
     * @param ipAddress 접속 IP
     * @param userAgent User-Agent 문자열
     * @param failReason 실패 사유
     * @return 실패 로그인 이력
     */
    public static LoginHistory failure(String userId, String email,
                                       String ipAddress, String userAgent,
                                       String failReason) {
        LoginHistory history = new LoginHistory();
        history.userId = userId;
        history.email = email;
        history.loginAt = LocalDateTime.now();
        history.ipAddress = ipAddress;
        history.userAgent = userAgent;
        history.success = false;
        history.failReason = failReason;
        return history;
    }

    /**
     * DB 복원용 빌더
     * <p>
     * Repository에서 Entity → Domain 변환 시 사용합니다.
     * </p>
     *
     * @return LoginHistoryRestoreBuilder
     */
    public static LoginHistoryRestoreBuilder restoreBuilder() {
        return new LoginHistoryRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 여부 확인
     *
     * @return loginHistoryId가 null이면 true
     */
    public boolean isNew() {
        return this.loginHistoryId == null;
    }

    /**
     * 성공 여부 확인
     *
     * @return 로그인 성공이면 true
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 실패 여부 확인
     *
     * @return 로그인 실패이면 true
     */
    public boolean isFailure() {
        return !this.success;
    }

    // ========================================
    // 비즈니스 메서드 없음 (Append-only)
    // ========================================
    // 이 도메인 모델은 생성 후 수정이 불가능합니다.
    // 상태 변경 메서드를 의도적으로 제공하지 않습니다.

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * DB 복원용 빌더
     * <p>
     * 모든 필드를 설정하여 DB의 기존 레코드를 도메인 객체로 복원합니다.
     * </p>
     */
    public static class LoginHistoryRestoreBuilder {
        private LoginHistoryId loginHistoryId;
        private String userId;
        private String email;
        private LocalDateTime loginAt;
        private String ipAddress;
        private String userAgent;
        private boolean success;
        private String failReason;

        public LoginHistoryRestoreBuilder loginHistoryId(LoginHistoryId loginHistoryId) {
            this.loginHistoryId = loginHistoryId;
            return this;
        }

        public LoginHistoryRestoreBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public LoginHistoryRestoreBuilder email(String email) {
            this.email = email;
            return this;
        }

        public LoginHistoryRestoreBuilder loginAt(LocalDateTime loginAt) {
            this.loginAt = loginAt;
            return this;
        }

        public LoginHistoryRestoreBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public LoginHistoryRestoreBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public LoginHistoryRestoreBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public LoginHistoryRestoreBuilder failReason(String failReason) {
            this.failReason = failReason;
            return this;
        }

        public LoginHistory build() {
            LoginHistory history = new LoginHistory();
            history.loginHistoryId = this.loginHistoryId;
            history.userId = this.userId;
            history.email = this.email;
            history.loginAt = this.loginAt;
            history.ipAddress = this.ipAddress;
            history.userAgent = this.userAgent;
            history.success = this.success;
            history.failReason = this.failReason;
            return history;
        }
    }
}