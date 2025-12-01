package com.jun_bank.auth_server.domain.history.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 이력 도메인 에러 코드
 * <p>
 * 로그인 이력 관련 에러를 정의합니다.
 * Append-only 특성상 에러가 거의 발생하지 않습니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>LGH_030~039: 유효성 검증 오류 (400 Bad Request)</li>
 * </ul>
 *
 * @see HistoryException
 * @see ErrorCode
 */
@Getter
@RequiredArgsConstructor
public enum HistoryErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 로그인 이력 ID 형식
     */
    INVALID_LOGIN_HISTORY_ID_FORMAT("LGH_030", "유효하지 않은 로그인 이력 ID 형식입니다", 400);

    private final String code;
    private final String message;
    private final int status;
}