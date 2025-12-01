package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;

import java.util.regex.Pattern;

/**
 * 비밀번호 VO (Value Object)
 * <p>
 * 암호화된 비밀번호를 저장하는 불변 객체입니다.
 * 평문 비밀번호는 이 VO에 저장되지 않으며,
 * {@link #validateRawPassword(String)}로 정책을 검증합니다.
 *
 * <h3>비밀번호 정책:</h3>
 * <ul>
 *   <li>최소 8자, 최대 100자</li>
 *   <li>영문자(대문자 또는 소문자) 1개 이상 포함</li>
 *   <li>숫자 1개 이상 포함</li>
 *   <li>특수문자(@$!%*?&) 1개 이상 포함</li>
 * </ul>
 *
 * <h3>사용 흐름:</h3>
 * <pre>{@code
 * // 1. 회원가입/비밀번호 변경 시 평문 검증
 * Password.validateRawPassword("Test1234!");  // 정책 위반 시 예외
 *
 * // 2. 암호화 (Application Layer에서)
 * String encoded = passwordEncoder.encode("Test1234!");
 *
 * // 3. VO 생성
 * Password password = Password.of(encoded);
 *
 * // 4. 로그인 시 검증 (Application Layer에서)
 * passwordEncoder.matches(rawPassword, password.encodedValue());
 * }</pre>
 *
 * <h3>보안 주의사항:</h3>
 * <ul>
 *   <li>평문 비밀번호는 절대 저장하지 않음</li>
 *   <li>toString()은 "[PROTECTED]" 반환</li>
 *   <li>암호화에는 BCrypt 사용 권장</li>
 * </ul>
 *
 * @param encodedValue 암호화된 비밀번호 (BCrypt 등)
 */
public record Password(String encodedValue) {

    /**
     * 평문 비밀번호 최소 길이
     */
    private static final int MIN_LENGTH = 8;

    /**
     * 평문 비밀번호 최대 길이
     */
    private static final int MAX_LENGTH = 100;

    /**
     * 비밀번호 정책 패턴
     * <p>
     * 영문자 1개 이상, 숫자 1개 이상, 특수문자(@$!%*?&) 1개 이상,
     * 총 8자 이상
     * </p>
     */
    private static final Pattern PASSWORD_POLICY_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    /**
     * Password 생성자 (Compact Constructor)
     * <p>
     * 암호화된 비밀번호의 존재 여부만 검증합니다.
     * 암호화된 값의 형식은 검증하지 않습니다.
     * </p>
     *
     * @param encodedValue 암호화된 비밀번호
     * @throws AuthException 암호화된 비밀번호가 없는 경우 (AUTH_031)
     */
    public Password {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw AuthException.invalidPasswordFormat();
        }
    }

    /**
     * 암호화된 비밀번호로 VO 생성
     * <p>
     * PasswordEncoder로 암호화한 후 이 메서드를 호출합니다.
     * </p>
     *
     * @param encodedValue BCrypt 등으로 암호화된 비밀번호
     * @return Password 객체
     * @throws AuthException 암호화된 비밀번호가 없는 경우
     */
    public static Password of(String encodedValue) {
        return new Password(encodedValue);
    }

    /**
     * 평문 비밀번호 정책 검증
     * <p>
     * 암호화 전에 평문 비밀번호가 정책을 만족하는지 검증합니다.
     * Application Layer의 회원가입/비밀번호 변경 로직에서 호출합니다.
     * </p>
     *
     * <h4>정책:</h4>
     * <ul>
     *   <li>8~100자</li>
     *   <li>영문자 1개 이상</li>
     *   <li>숫자 1개 이상</li>
     *   <li>특수문자(@$!%*?&) 1개 이상</li>
     * </ul>
     *
     * @param rawPassword 평문 비밀번호
     * @throws AuthException 정책 위반 시 (AUTH_031)
     */
    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null) {
            throw AuthException.invalidPasswordFormat();
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw AuthException.invalidPasswordFormat();
        }
        if (!PASSWORD_POLICY_PATTERN.matcher(rawPassword).matches()) {
            throw AuthException.invalidPasswordFormat();
        }
    }

    /**
     * 평문 비밀번호 정책 만족 여부 확인 (예외 없이)
     * <p>
     * 검증 결과만 반환하고 예외를 발생시키지 않습니다.
     * UI에서 실시간 유효성 검사에 사용할 수 있습니다.
     * </p>
     *
     * @param rawPassword 평문 비밀번호
     * @return 정책 만족 시 true
     */
    public static boolean isValidRawPassword(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            return false;
        }
        return PASSWORD_POLICY_PATTERN.matcher(rawPassword).matches();
    }

    /**
     * toString 오버라이드
     * <p>
     * 보안을 위해 암호화된 값도 노출하지 않습니다.
     * 로그에 비밀번호가 출력되는 것을 방지합니다.
     * </p>
     *
     * @return "[PROTECTED]"
     */
    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}