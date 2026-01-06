package com.jun_bank.auth_server.domain.token.domain.model.vo;

import com.jun_bank.auth_server.domain.token.domain.exception.TokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RefreshTokenId VO 테스트")
class RefreshTokenIdTest {

    @Test
    @DisplayName("유효한 ID 생성")
    void create_ValidId() {
        RefreshTokenId id = RefreshTokenId.of("RTK-12345678");

        assertThat(id.value()).isEqualTo("RTK-12345678");
    }

    @Test
    @DisplayName("ID 자동 생성")
    void generateId() {
        String id = RefreshTokenId.generateId();

        assertThat(id).startsWith("RTK-");
        assertThat(id).hasSize(12);
    }

    @Test
    @DisplayName("자동 생성 ID는 유효한 형식")
    void generateId_IsValid() {
        String generated = RefreshTokenId.generateId();

        assertThatCode(() -> RefreshTokenId.of(generated))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "AUT-12345678", "RTK12345678", "RTK-short", "rtk-12345678"})
    @DisplayName("유효하지 않은 ID 형식 시 예외")
    void create_InvalidId_ThrowsException(String invalidId) {
        assertThatThrownBy(() -> RefreshTokenId.of(invalidId))
                .isInstanceOf(TokenException.class);
    }

    @Test
    @DisplayName("null ID 시 예외")
    void create_NullId_ThrowsException() {
        assertThatThrownBy(() -> RefreshTokenId.of(null))
                .isInstanceOf(TokenException.class);
    }

    @Test
    @DisplayName("PREFIX 상수 확인")
    void prefix_IsRTK() {
        assertThat(RefreshTokenId.PREFIX).isEqualTo("RTK");
    }
}