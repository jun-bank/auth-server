package com.jun_bank.auth_server.domain.token.domain.model.vo;

import com.jun_bank.auth_server.domain.token.domain.exception.TokenException;
import com.jun_bank.common_lib.util.UuidUtils;

/**
 * 리프레시 토큰 식별자 VO (Value Object)
 * <p>
 * 리프레시 토큰 레코드의 고유 식별자입니다.
 * 토큰 값(JWT) 자체와는 별개로 DB 레코드를 식별하는 용도입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>RTK-xxxxxxxx (예: RTK-a1b2c3d4)</pre>
 * <ul>
 *   <li>RTK: 리프레시 토큰 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * <h3>토큰 값과의 차이:</h3>
 * <ul>
 *   <li>RefreshTokenId: DB 레코드 식별자 (RTK-xxx)</li>
 *   <li>token (String): 실제 JWT 토큰 값</li>
 * </ul>
 *
 * @param value 리프레시 토큰 ID 문자열 (RTK-xxxxxxxx 형식)
 */
public record RefreshTokenId(String value) {

    /**
     * ID 프리픽스
     * <p>모든 리프레시 토큰 ID는 "RTK-"로 시작합니다.</p>
     */
    public static final String PREFIX = "RTK";

    /**
     * RefreshTokenId 생성자 (Compact Constructor)
     * <p>
     * ID 형식을 검증하고, 유효하지 않으면 예외를 발생시킵니다.
     * </p>
     *
     * @param value 리프레시 토큰 ID 문자열
     * @throws TokenException ID 형식이 유효하지 않은 경우 (TKN_030)
     */
    public RefreshTokenId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw TokenException.invalidRefreshTokenIdFormat(value);
        }
    }

    /**
     * 문자열로부터 RefreshTokenId 객체 생성
     *
     * @param value 리프레시 토큰 ID 문자열
     * @return RefreshTokenId 객체
     * @throws TokenException ID 형식이 유효하지 않은 경우
     */
    public static RefreshTokenId of(String value) {
        return new RefreshTokenId(value);
    }

    /**
     * 새로운 리프레시 토큰 ID 생성
     *
     * @return 생성된 ID 문자열 (RTK-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}