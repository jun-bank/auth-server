package com.jun_bank.auth_server.domain.token.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

/**
 * 토큰 도메인 예외
 * <p>
 * JWT 토큰 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 *
 * @see TokenErrorCode
 * @see BusinessException
 */
public class TokenException extends BusinessException {

    public TokenException(TokenErrorCode errorCode) {
        super(errorCode);
    }

    public TokenException(TokenErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 토큰 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 토큰 예외 생성
     * <p>
     * 토큰 형식이 잘못되었거나 서명 검증에 실패한 경우 발생합니다.
     * </p>
     *
     * @return TokenException 인스턴스
     */
    public static TokenException invalidToken() {
        return new TokenException(TokenErrorCode.INVALID_TOKEN);
    }

    /**
     * 만료된 토큰 예외 생성
     * <p>
     * 토큰의 exp 클레임이 현재 시간보다 이전인 경우 발생합니다.
     * </p>
     *
     * @return TokenException 인스턴스
     */
    public static TokenException tokenExpired() {
        return new TokenException(TokenErrorCode.TOKEN_EXPIRED);
    }

    /**
     * 폐기된 토큰 예외 생성
     * <p>
     * 로그아웃 등으로 명시적으로 폐기된 토큰인 경우 발생합니다.
     * </p>
     *
     * @return TokenException 인스턴스
     */
    public static TokenException tokenRevoked() {
        return new TokenException(TokenErrorCode.TOKEN_REVOKED);
    }

    // ========================================
    // 토큰 조회 관련 팩토리 메서드
    // ========================================

    /**
     * 리프레시 토큰을 찾을 수 없음 예외 생성
     * <p>
     * 토큰 갱신 요청 시 해당 리프레시 토큰이 DB에 없는 경우 발생합니다.
     * </p>
     *
     * @param token 찾을 수 없는 토큰 (마스킹하여 로깅)
     * @return TokenException 인스턴스
     */
    public static TokenException refreshTokenNotFound(String token) {
        return new TokenException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND,
                "token=" + maskToken(token));
    }

    /**
     * 토큰/세션을 찾을 수 없음 예외 생성
     * <p>
     * 세션 ID로 토큰을 조회할 수 없는 경우 발생합니다.
     * </p>
     *
     * @return TokenException 인스턴스
     */
    public static TokenException tokenNotFound() {
        return new TokenException(TokenErrorCode.SESSION_NOT_FOUND);
    }

    // ========================================
    // 권한 관련 팩토리 메서드
    // ========================================

    /**
     * 권한 없음 예외 생성
     * <p>
     * 타인의 세션에 접근하려는 경우 발생합니다.
     * </p>
     *
     * @return TokenException 인스턴스
     */
    public static TokenException unauthorized() {
        return new TokenException(TokenErrorCode.UNAUTHORIZED_ACCESS);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 리프레시 토큰 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return TokenException 인스턴스
     */
    public static TokenException invalidRefreshTokenIdFormat(String id) {
        return new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN_ID_FORMAT, "id=" + id);
    }

    // ========================================
    // Private 유틸리티 메서드
    // ========================================

    /**
     * 토큰 마스킹
     * <p>
     * 보안을 위해 토큰의 일부만 표시합니다.
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