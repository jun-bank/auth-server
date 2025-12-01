package com.jun_bank.auth_server.domain.history.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

/**
 * 로그인 이력 도메인 예외
 * <p>
 * 로그인 이력 관련 예외를 처리합니다.
 * Append-only 특성상 예외가 거의 발생하지 않습니다.
 *
 * @see HistoryErrorCode
 * @see BusinessException
 */
public class HistoryException extends BusinessException {

    public HistoryException(HistoryErrorCode errorCode) {
        super(errorCode);
    }

    public HistoryException(HistoryErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 로그인 이력 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return HistoryException 인스턴스
     */
    public static HistoryException invalidLoginHistoryIdFormat(String id) {
        return new HistoryException(HistoryErrorCode.INVALID_LOGIN_HISTORY_ID_FORMAT, "id=" + id);
    }
}