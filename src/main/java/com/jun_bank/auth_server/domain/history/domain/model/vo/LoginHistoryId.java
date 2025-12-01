package com.jun_bank.auth_server.domain.history.domain.model.vo;

import com.jun_bank.auth_server.domain.history.domain.exception.HistoryException;
import com.jun_bank.common_lib.util.UuidUtils;

/**
 * 로그인 이력 식별자 VO (Value Object)
 * <p>
 * 로그인 시도 기록의 고유 식별자입니다.
 * Append-only 테이블에서 각 로그인 시도를 식별합니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>LGH-xxxxxxxx (예: LGH-a1b2c3d4)</pre>
 * <ul>
 *   <li>LGH: 로그인 이력 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * @param value 로그인 이력 ID 문자열 (LGH-xxxxxxxx 형식)
 */
public record LoginHistoryId(String value) {

    /**
     * ID 프리픽스
     * <p>모든 로그인 이력 ID는 "LGH-"로 시작합니다.</p>
     */
    public static final String PREFIX = "LGH";

    /**
     * LoginHistoryId 생성자 (Compact Constructor)
     * <p>
     * ID 형식을 검증하고, 유효하지 않으면 예외를 발생시킵니다.
     * </p>
     *
     * @param value 로그인 이력 ID 문자열
     * @throws HistoryException ID 형식이 유효하지 않은 경우 (LGH_030)
     */
    public LoginHistoryId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw HistoryException.invalidLoginHistoryIdFormat(value);
        }
    }

    /**
     * 문자열로부터 LoginHistoryId 객체 생성
     *
     * @param value 로그인 이력 ID 문자열
     * @return LoginHistoryId 객체
     * @throws HistoryException ID 형식이 유효하지 않은 경우
     */
    public static LoginHistoryId of(String value) {
        return new LoginHistoryId(value);
    }

    /**
     * 새로운 로그인 이력 ID 생성
     *
     * @return 생성된 ID 문자열 (LGH-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}