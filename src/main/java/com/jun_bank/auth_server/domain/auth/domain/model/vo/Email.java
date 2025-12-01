package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;

import java.util.regex.Pattern;

/**
 * 이메일 VO (Value Object) - Auth Server
 * <p>
 * 인증에 사용되는 이메일 주소를 표현하는 불변 객체입니다.
 * User Service의 Email VO와 동일한 검증 로직을 사용하지만,
 * Auth Server의 예외 체계를 따릅니다.
 *
 * <h3>검증 규칙:</h3>
 * <ul>
 *   <li>null이거나 빈 문자열 불가</li>
 *   <li>최대 255자</li>
 *   <li>RFC 5322 기반 형식 검증</li>
 *   <li>소문자로 정규화하여 저장</li>
 * </ul>
 *
 * <h3>User Service Email과의 관계:</h3>
 * <p>
 * 회원가입 시 User Service에서 Auth Server로 동일한 이메일이 전달됩니다.
 * 양쪽 서비스에서 동일한 검증 규칙을 적용합니다.
 * </p>
 *
 * @param value 이메일 주소 문자열 (소문자로 정규화됨)
 */
public record Email(String value) {

    /**
     * 이메일 정규식 패턴
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 최대 길이
     */
    private static final int MAX_LENGTH = 255;

    /**
     * Email 생성자 (Compact Constructor)
     * <p>
     * 이메일 형식을 검증하고 소문자로 정규화합니다.
     * </p>
     *
     * @param value 이메일 주소 문자열
     * @throws AuthException 이메일 형식이 유효하지 않은 경우 (AUTH_030)
     */
    public Email {
        if (value == null || value.isBlank()) {
            throw AuthException.invalidEmailFormat(value);
        }
        if (value.length() > MAX_LENGTH) {
            throw AuthException.invalidEmailFormat(value);
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw AuthException.invalidEmailFormat(value);
        }
        // 소문자로 정규화
        value = value.toLowerCase();
    }

    /**
     * 문자열로부터 Email 객체 생성
     *
     * @param value 이메일 주소 문자열
     * @return Email 객체
     * @throws AuthException 이메일 형식이 유효하지 않은 경우
     */
    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * 마스킹된 이메일 반환
     * <p>
     * 개인정보 보호를 위해 로컬 파트의 일부를 마스킹합니다.
     * 로그 출력이나 에러 메시지에 사용합니다.
     * </p>
     *
     * @return 마스킹된 이메일 (예: "u***r@example.com")
     */
    public String masked() {
        int atIndex = value.indexOf('@');
        String local = value.substring(0, atIndex);
        String domain = value.substring(atIndex);

        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}