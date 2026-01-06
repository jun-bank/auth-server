package com.jun_bank.auth_server.domain.auth.domain.model.vo;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthUserId VO 테스트")
class AuthUserIdTest {

    @Test
    @DisplayName("유효한 ID 생성")
    void create_ValidId() {
        AuthUserId id = AuthUserId.of("AUT-12345678");

        assertThat(id.value()).isEqualTo("AUT-12345678");
    }

    @Test
    @DisplayName("ID 자동 생성")
    void generateId() {
        String id = AuthUserId.generateId();

        assertThat(id).startsWith("AUT-");
        assertThat(id).hasSize(12); // AUT- + 8자리
    }

    @Test
    @DisplayName("자동 생성 ID는 유효한 형식")
    void generateId_IsValid() {
        String generated = AuthUserId.generateId();

        assertThatCode(() -> AuthUserId.of(generated))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "USR-12345678", "AUT12345678", "AUT-short", "aut-12345678"})
    @DisplayName("유효하지 않은 ID 형식 시 예외")
    void create_InvalidId_ThrowsException(String invalidId) {
        assertThatThrownBy(() -> AuthUserId.of(invalidId))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("null ID 시 예외")
    void create_NullId_ThrowsException() {
        assertThatThrownBy(() -> AuthUserId.of(null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("PREFIX 상수 확인")
    void prefix_IsAUT() {
        assertThat(AuthUserId.PREFIX).isEqualTo("AUT");
    }
}