package com.jun_bank.auth_server.domain.auth.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

/**
 * 인증 도메인 예외
 * <p>
 * 인증/인가 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 * {@link AuthErrorCode}를 기반으로 예외를 생성합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 에러 코드만으로 예외 생성
 * throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
 *
 * // 상세 메시지와 함께 예외 생성
 * throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, "남은 시간: 30분");
 *
 * // 팩토리 메서드 사용 (권장)
 * throw AuthException.invalidCredentials();
 * throw AuthException.accountLocked(30);
 * }</pre>
 *
 * @see AuthErrorCode
 * @see BusinessException
 */
public class AuthException extends BusinessException {

    /**
     * 에러 코드로 예외 생성
     *
     * @param errorCode 인증 에러 코드
     */
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지로 예외 생성
     *
     * @param errorCode 인증 에러 코드
     * @param detailMessage 상세 메시지
     */
    public AuthException(AuthErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 인증 실패 관련 팩토리 메서드
    // ========================================

    /**
     * 잘못된 인증 정보 예외 생성
     * <p>
     * 이메일 또는 비밀번호가 일치하지 않는 경우 발생합니다.
     * 보안을 위해 어떤 정보가 틀렸는지 구체적으로 알려주지 않습니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException invalidCredentials() {
        return new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    /**
     * 비밀번호 불일치 예외 생성
     * <p>
     * 비밀번호 변경 시 현재 비밀번호가 일치하지 않는 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException passwordMismatch() {
        return new AuthException(AuthErrorCode.PASSWORD_MISMATCH);
    }

    // ========================================
    // 토큰 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 토큰 예외 생성
     * <p>
     * 토큰 형식이 잘못되었거나 서명 검증에 실패한 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException invalidToken() {
        return new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    /**
     * 만료된 토큰 예외 생성
     * <p>
     * 토큰의 exp 클레임이 현재 시간보다 이전인 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException tokenExpired() {
        return new AuthException(AuthErrorCode.TOKEN_EXPIRED);
    }

    /**
     * 폐기된 토큰 예외 생성
     * <p>
     * 로그아웃 등으로 명시적으로 폐기된 토큰인 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException tokenRevoked() {
        return new AuthException(AuthErrorCode.TOKEN_REVOKED);
    }

    /**
     * 리프레시 토큰을 찾을 수 없음 예외 생성
     * <p>
     * 토큰 갱신 요청 시 해당 리프레시 토큰이 DB에 없는 경우 발생합니다.
     * </p>
     *
     * @param token 찾을 수 없는 토큰 (마스킹하여 로깅)
     * @return AuthException 인스턴스
     */
    public static AuthException refreshTokenNotFound(String token) {
        return new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
                "token=" + maskToken(token));
    }

    // ========================================
    // 계정 상태 관련 팩토리 메서드
    // ========================================

    /**
     * 계정 잠김 예외 생성
     * <p>
     * 로그인 실패 횟수 초과로 계정이 잠긴 경우 발생합니다.
     * 남은 잠금 시간을 함께 전달합니다.
     * </p>
     *
     * @param remainingMinutes 남은 잠금 시간 (분)
     * @return AuthException 인스턴스
     */
    public static AuthException accountLocked(long remainingMinutes) {
        return new AuthException(AuthErrorCode.ACCOUNT_LOCKED,
                String.format("남은 시간: %d분", remainingMinutes));
    }

    /**
     * 계정 비활성화 예외 생성
     * <p>
     * 관리자에 의해 비활성화된 계정으로 로그인을 시도하는 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException accountDisabled() {
        return new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    /**
     * 인증 사용자를 찾을 수 없음 예외 생성
     * <p>
     * 해당 이메일 또는 ID의 인증 정보가 없는 경우 발생합니다.
     * </p>
     *
     * @param identifier 이메일 또는 ID
     * @return AuthException 인스턴스
     */
    public static AuthException authUserNotFound(String identifier) {
        return new AuthException(AuthErrorCode.AUTH_USER_NOT_FOUND,
                "identifier=" + identifier);
    }

    /**
     * 이미 존재하는 인증 사용자 예외 생성
     * <p>
     * 회원가입 시 이미 등록된 이메일인 경우 발생합니다.
     * </p>
     *
     * @param email 중복된 이메일
     * @return AuthException 인스턴스
     */
    public static AuthException authUserAlreadyExists(String email) {
        return new AuthException(AuthErrorCode.AUTH_USER_ALREADY_EXISTS,
                "email=" + email);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 이메일 형식 예외 생성
     *
     * @param email 유효하지 않은 이메일
     * @return AuthException 인스턴스
     */
    public static AuthException invalidEmailFormat(String email) {
        return new AuthException(AuthErrorCode.INVALID_EMAIL_FORMAT, "email=" + email);
    }

    /**
     * 유효하지 않은 비밀번호 형식 예외 생성
     * <p>
     * 비밀번호가 정책(8자 이상, 영문/숫자/특수문자 포함)을 만족하지 않는 경우 발생합니다.
     * </p>
     *
     * @return AuthException 인스턴스
     */
    public static AuthException invalidPasswordFormat() {
        return new AuthException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
    }

    /**
     * 유효하지 않은 인증 사용자 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return AuthException 인스턴스
     */
    public static AuthException invalidAuthUserIdFormat(String id) {
        return new AuthException(AuthErrorCode.INVALID_AUTH_USER_ID_FORMAT, "id=" + id);
    }

    /**
     * 유효하지 않은 리프레시 토큰 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return AuthException 인스턴스
     */
    public static AuthException invalidRefreshTokenIdFormat(String id) {
        return new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN_ID_FORMAT, "id=" + id);
    }

    /**
     * 유효하지 않은 로그인 이력 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return AuthException 인스턴스
     */
    public static AuthException invalidLoginHistoryIdFormat(String id) {
        return new AuthException(AuthErrorCode.INVALID_LOGIN_HISTORY_ID_FORMAT, "id=" + id);
    }

    // ========================================
    // Private 유틸리티 메서드
    // ========================================

    /**
     * 토큰 마스킹
     * <p>
     * 보안을 위해 토큰의 일부만 표시합니다.
     * 로그에 토큰 전체가 노출되는 것을 방지합니다.
     * </p>
     *
     * @param token 원본 토큰
     * @return 마스킹된 토큰 (예: "eyJhb...xyz123")
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 6);
    }
}