package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import com.jun_bank.common_lib.util.UuidUtils;

/**
 * 인증 사용자 식별자 VO (Value Object)
 * <p>
 * Auth Server에서 관리하는 사용자 인증 정보의 고유 식별자입니다.
 * User Service의 UserId와는 별개로 관리됩니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>AUT-xxxxxxxx (예: AUT-a1b2c3d4)</pre>
 * <ul>
 *   <li>AUT: 인증 사용자 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * <h3>User Service UserId와의 관계:</h3>
 * <p>
 * AuthUser는 별도로 User Service의 UserId(USR-xxx)를 참조합니다.
 * AuthUserId는 Auth Server 내부에서만 사용되며,
 * 서비스 간 통신에서는 UserId를 사용합니다.
 * </p>
 *
 * @param value 인증 사용자 ID 문자열 (AUT-xxxxxxxx 형식)
 * @see com.jun_bank.common_lib.util.UuidUtils
 */
public record AuthUserId(String value) {

    /**
     * ID 프리픽스
     * <p>모든 인증 사용자 ID는 "AUT-"로 시작합니다.</p>
     */
    public static final String PREFIX = "AUT";

    /**
     * AuthUserId 생성자 (Compact Constructor)
     * <p>
     * ID 형식을 검증하고, 유효하지 않으면 예외를 발생시킵니다.
     * </p>
     *
     * @param value 인증 사용자 ID 문자열
     * @throws AuthException ID 형식이 유효하지 않은 경우 (AUTH_032)
     */
    public AuthUserId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw AuthException.invalidAuthUserIdFormat(value);
        }
    }

    /**
     * 문자열로부터 AuthUserId 객체 생성
     *
     * @param value 인증 사용자 ID 문자열
     * @return AuthUserId 객체
     * @throws AuthException ID 형식이 유효하지 않은 경우
     */
    public static AuthUserId of(String value) {
        return new AuthUserId(value);
    }

    /**
     * 새로운 인증 사용자 ID 생성
     * <p>
     * Entity 레이어에서 새 인증 사용자를 저장할 때 호출합니다.
     * </p>
     *
     * @return 생성된 ID 문자열 (AUT-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}