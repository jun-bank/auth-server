package com.jun_bank.auth_server.domain.history.domain.model.vo;

import com.jun_bank.auth_server.domain.history.domain.exception.HistoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginHistoryId VO 테스트")
class LoginHistoryIdTest {

    @Test
    @DisplayName("유효한 ID 생성")
    void create_ValidId() {
        LoginHistoryId id = LoginHistoryId.of("LGH-12345678");

        assertThat(id.value()).isEqualTo("LGH-12345678");
    }

    @Test
    @DisplayName("ID 자동 생성")
    void generateId() {
        String id = LoginHistoryId.generateId();

        assertThat(id).startsWith("LGH-");
        assertThat(id).hasSize(12);
    }

    @Test
    @DisplayName("자동 생성 ID는 유효한 형식")
    void generateId_IsValid() {
        String generated = LoginHistoryId.generateId();

        assertThatCode(() -> LoginHistoryId.of(generated))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "AUT-12345678", "LGH12345678", "LGH-short", "lgh-12345678"})
    @DisplayName("유효하지 않은 ID 형식 시 예외")
    void create_InvalidId_ThrowsException(String invalidId) {
        assertThatThrownBy(() -> LoginHistoryId.of(invalidId))
                .isInstanceOf(HistoryException.class);
    }

    @Test
    @DisplayName("null ID 시 예외")
    void create_NullId_ThrowsException() {
        assertThatThrownBy(() -> LoginHistoryId.of(null))
                .isInstanceOf(HistoryException.class);
    }

    @Test
    @DisplayName("PREFIX 상수 확인")
    void prefix_IsLGH() {
        assertThat(LoginHistoryId.PREFIX).isEqualTo("LGH");
    }
}