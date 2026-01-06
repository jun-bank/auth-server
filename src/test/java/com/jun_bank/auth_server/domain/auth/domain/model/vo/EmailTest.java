package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email VO 테스트")
class EmailTest {

    @Test
    @DisplayName("유효한 이메일 생성 - 소문자로 정규화")
    void create_ValidEmail() {
        Email email = Email.of("Test@Example.COM");

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "no@domain", "@example.com", "test@.com"})
    @DisplayName("유효하지 않은 이메일 형식 시 예외")
    void create_InvalidEmail_ThrowsException(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("null 이메일 시 예외")
    void create_NullEmail_ThrowsException() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("마스킹 처리")
    void masked() {
        Email email = Email.of("user@example.com");

        assertThat(email.masked()).isEqualTo("u***r@example.com");
    }

    @Test
    @DisplayName("짧은 로컬 파트 마스킹")
    void masked_ShortLocal() {
        Email email = Email.of("ab@example.com");

        assertThat(email.masked()).isEqualTo("a***@example.com");
    }
}