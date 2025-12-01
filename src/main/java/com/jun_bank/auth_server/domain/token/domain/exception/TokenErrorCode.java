package com.jun_bank.auth_server.domain.token.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토큰 도메인 에러 코드
 * <p>
 * JWT 토큰 관련 비즈니스 로직에서 발생할 수 있는 모든 에러를 정의합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>TKN_001~009: 토큰 검증 오류 (401 Unauthorized)</li>
 *   <li>TKN_010~019: 토큰 조회 오류 (404 Not Found)</li>
 *   <li>TKN_030~039: 유효성 검증 오류 (400 Bad Request)</li>
 * </ul>
 *
 * @see TokenException
 * @see ErrorCode
 */
@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {

    // ========================================
    // 토큰 검증 오류 (401 Unauthorized)
    // ========================================

    /**
     * 유효하지 않은 토큰
     * <p>토큰 형식이 잘못되었거나 서명이 유효하지 않은 경우</p>
     */
    INVALID_TOKEN("TKN_001", "유효하지 않은 토큰입니다", 401),

    /**
     * 만료된 토큰
     * <p>토큰의 유효 기간이 지난 경우</p>
     */
    TOKEN_EXPIRED("TKN_002", "토큰이 만료되었습니다", 401),

    /**
     * 폐기된 토큰
     * <p>로그아웃 등으로 폐기된 토큰인 경우</p>
     */
    TOKEN_REVOKED("TKN_003", "폐기된 토큰입니다", 401),

    // ========================================
    // 토큰 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 리프레시 토큰을 찾을 수 없음
     * <p>DB에 해당 리프레시 토큰이 존재하지 않는 경우</p>
     */
    REFRESH_TOKEN_NOT_FOUND("TKN_010", "리프레시 토큰을 찾을 수 없습니다", 404),

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 리프레시 토큰 ID 형식
     */
    INVALID_REFRESH_TOKEN_ID_FORMAT("TKN_030", "유효하지 않은 리프레시 토큰 ID 형식입니다", 400);

    private final String code;
    private final String message;
    private final int status;
}