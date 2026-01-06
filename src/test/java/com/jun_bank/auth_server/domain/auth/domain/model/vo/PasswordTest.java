package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Password VO 테스트")
class PasswordTest {

    @Test
    @DisplayName("암호화된 비밀번호로 생성")
    void create_EncodedPassword() {
        Password password = Password.of("$2a$10$encodedValue");

        assertThat(password.encodedValue()).isEqualTo("$2a$10$encodedValue");
    }

    @Test
    @DisplayName("빈 비밀번호 시 예외")
    void create_EmptyPassword_ThrowsException() {
        assertThatThrownBy(() -> Password.of(""))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("null 비밀번호 시 예외")
    void create_NullPassword_ThrowsException() {
        assertThatThrownBy(() -> Password.of(null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("toString은 [PROTECTED] 반환")
    void toString_ReturnsProtected() {
        Password password = Password.of("secret");

        assertThat(password.toString()).isEqualTo("[PROTECTED]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Test1234!", "Password@1", "Abcd1234!"})
    @DisplayName("유효한 평문 비밀번호 정책 검증")
    void validateRawPassword_Valid(String validPassword) {
        assertThatCode(() -> Password.validateRawPassword(validPassword))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "nouppercase!", "NOLOWERCASE!", "NoNumber!@", "NoSpecial1a"})
    @DisplayName("정책 위반 평문 비밀번호 시 예외")
    void validateRawPassword_Invalid_ThrowsException(String invalidPassword) {
        assertThatThrownBy(() -> Password.validateRawPassword(invalidPassword))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("isValidRawPassword - 유효한 경우 true")
    void isValidRawPassword_Valid_ReturnsTrue() {
        assertThat(Password.isValidRawPassword("Test1234!")).isTrue();
    }

    @Test
    @DisplayName("isValidRawPassword - 무효한 경우 false")
    void isValidRawPassword_Invalid_ReturnsFalse() {
        assertThat(Password.isValidRawPassword("short")).isFalse();
    }
}