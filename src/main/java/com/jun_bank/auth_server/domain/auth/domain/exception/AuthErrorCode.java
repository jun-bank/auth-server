package com.jun_bank.auth_server.domain.auth.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 도메인 에러 코드
 * <p>
 * Auth Server에서 발생할 수 있는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 * {@link com.jun_bank.common_lib.exception.ErrorCode} 인터페이스를 구현하여
 * 전역 예외 핸들러에서 일관된 응답을 생성할 수 있습니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>AUTH_001~009: 인증 실패 오류 (401 Unauthorized)</li>
 *   <li>AUTH_010~019: 토큰 관련 오류 (401 Unauthorized)</li>
 *   <li>AUTH_020~024: 계정 상태 오류 (403 Forbidden)</li>
 *   <li>AUTH_025~029: 조회 오류 (404 Not Found)</li>
 *   <li>AUTH_030~039: 유효성 검증 오류 (400 Bad Request)</li>
 *   <li>AUTH_040~049: 중복 오류 (409 Conflict)</li>
 * </ul>
 *
 * @see AuthException
 * @see com.jun_bank.common_lib.exception.ErrorCode
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // ========================================
    // 인증 실패 (401 Unauthorized)
    // ========================================

    /**
     * 잘못된 인증 정보
     * <p>이메일 또는 비밀번호가 일치하지 않는 경우</p>
     */
    INVALID_CREDENTIALS("AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다", 401),

    /**
     * 비밀번호 불일치
     * <p>비밀번호 변경 시 현재 비밀번호가 일치하지 않는 경우</p>
     */
    PASSWORD_MISMATCH("AUTH_002", "비밀번호가 일치하지 않습니다", 401),

    /**
     * 인증 필요
     * <p>인증되지 않은 요청인 경우</p>
     */
    AUTHENTICATION_REQUIRED("AUTH_003", "인증이 필요합니다", 401),

    // ========================================
    // 토큰 관련 (401 Unauthorized)
    // ========================================

    /**
     * 유효하지 않은 토큰
     * <p>토큰 형식이 잘못되었거나 서명이 유효하지 않은 경우</p>
     */
    INVALID_TOKEN("AUTH_010", "유효하지 않은 토큰입니다", 401),

    /**
     * 만료된 토큰
     * <p>토큰의 유효 기간이 지난 경우</p>
     */
    TOKEN_EXPIRED("AUTH_011", "토큰이 만료되었습니다", 401),

    /**
     * 폐기된 토큰
     * <p>로그아웃 등으로 폐기된 토큰인 경우</p>
     */
    TOKEN_REVOKED("AUTH_012", "폐기된 토큰입니다", 401),

    /**
     * 리프레시 토큰을 찾을 수 없음
     * <p>DB에 해당 리프레시 토큰이 존재하지 않는 경우</p>
     */
    REFRESH_TOKEN_NOT_FOUND("AUTH_013", "리프레시 토큰을 찾을 수 없습니다", 401),

    // ========================================
    // 계정 상태 (403 Forbidden)
    // ========================================

    /**
     * 계정 잠김
     * <p>로그인 실패 횟수 초과로 계정이 잠긴 경우</p>
     */
    ACCOUNT_LOCKED("AUTH_020", "계정이 잠겨있습니다. 잠시 후 다시 시도해주세요", 403),

    /**
     * 계정 비활성화
     * <p>관리자에 의해 비활성화된 계정인 경우</p>
     */
    ACCOUNT_DISABLED("AUTH_021", "비활성화된 계정입니다", 403),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 인증 사용자를 찾을 수 없음
     * <p>해당 이메일 또는 ID의 인증 정보가 없는 경우</p>
     */
    AUTH_USER_NOT_FOUND("AUTH_025", "인증 사용자를 찾을 수 없습니다", 404),

    // ========================================
    // 중복 오류 (409 Conflict)
    // ========================================

    /**
     * 이미 등록된 인증 정보
     * <p>회원가입 시 이미 등록된 이메일인 경우</p>
     */
    AUTH_USER_ALREADY_EXISTS("AUTH_026", "이미 등록된 인증 정보입니다", 409),

    // ========================================
    // 유효성 검증 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 이메일 형식
     */
    INVALID_EMAIL_FORMAT("AUTH_030", "유효하지 않은 이메일 형식입니다", 400),

    /**
     * 유효하지 않은 비밀번호 형식
     * <p>비밀번호 정책을 만족하지 않는 경우</p>
     */
    INVALID_PASSWORD_FORMAT("AUTH_031", "비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다", 400),

    /**
     * 유효하지 않은 인증 사용자 ID 형식
     */
    INVALID_AUTH_USER_ID_FORMAT("AUTH_032", "유효하지 않은 인증 사용자 ID 형식입니다", 400),

    /**
     * 유효하지 않은 리프레시 토큰 ID 형식
     */
    INVALID_REFRESH_TOKEN_ID_FORMAT("AUTH_033", "유효하지 않은 리프레시 토큰 ID 형식입니다", 400),

    /**
     * 유효하지 않은 로그인 이력 ID 형식
     */
    INVALID_LOGIN_HISTORY_ID_FORMAT("AUTH_034", "유효하지 않은 로그인 이력 ID 형식입니다", 400);

    private final String code;
    private final String message;
    private final int status;
}