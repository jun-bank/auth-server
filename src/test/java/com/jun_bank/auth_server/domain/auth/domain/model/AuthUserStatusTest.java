package com.jun_bank.auth_server.domain.auth.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuthUserStatus 테스트")
class AuthUserStatusTest {

    @Test
    @DisplayName("ACTIVE - 인증 가능")
    void active_CanAuthenticate() {
        AuthUserStatus status = AuthUserStatus.ACTIVE;

        assertThat(status.isActive()).isTrue();
        assertThat(status.canAuthenticate()).isTrue();
        assertThat(status.isLocked()).isFalse();
        assertThat(status.isDisabled()).isFalse();
    }

    @Test
    @DisplayName("LOCKED - 인증 불가")
    void locked_CannotAuthenticate() {
        AuthUserStatus status = AuthUserStatus.LOCKED;

        assertThat(status.isLocked()).isTrue();
        assertThat(status.canAuthenticate()).isFalse();
        assertThat(status.isActive()).isFalse();
        assertThat(status.isDisabled()).isFalse();
    }

    @Test
    @DisplayName("DISABLED - 인증 불가")
    void disabled_CannotAuthenticate() {
        AuthUserStatus status = AuthUserStatus.DISABLED;

        assertThat(status.isDisabled()).isTrue();
        assertThat(status.canAuthenticate()).isFalse();
        assertThat(status.isActive()).isFalse();
        assertThat(status.isLocked()).isFalse();
    }

    @Test
    @DisplayName("ACTIVE → LOCKED 전환 가능")
    void canTransition_ActiveToLocked() {
        assertThat(AuthUserStatus.ACTIVE.canTransitionTo(AuthUserStatus.LOCKED)).isTrue();
    }

    @Test
    @DisplayName("ACTIVE → DISABLED 전환 가능")
    void canTransition_ActiveToDisabled() {
        assertThat(AuthUserStatus.ACTIVE.canTransitionTo(AuthUserStatus.DISABLED)).isTrue();
    }

    @Test
    @DisplayName("LOCKED → ACTIVE 전환 가능")
    void canTransition_LockedToActive() {
        assertThat(AuthUserStatus.LOCKED.canTransitionTo(AuthUserStatus.ACTIVE)).isTrue();
    }

    @Test
    @DisplayName("DISABLED → ACTIVE 전환 가능")
    void canTransition_DisabledToActive() {
        assertThat(AuthUserStatus.DISABLED.canTransitionTo(AuthUserStatus.ACTIVE)).isTrue();
    }

    @Test
    @DisplayName("같은 상태로 전환 불가")
    void canTransition_SameStatus_ReturnsFalse() {
        assertThat(AuthUserStatus.ACTIVE.canTransitionTo(AuthUserStatus.ACTIVE)).isFalse();
        assertThat(AuthUserStatus.LOCKED.canTransitionTo(AuthUserStatus.LOCKED)).isFalse();
        assertThat(AuthUserStatus.DISABLED.canTransitionTo(AuthUserStatus.DISABLED)).isFalse();
    }

    @Test
    @DisplayName("getAllowedTransitions - ACTIVE")
    void getAllowedTransitions_Active() {
        assertThat(AuthUserStatus.ACTIVE.getAllowedTransitions())
                .containsExactlyInAnyOrder(AuthUserStatus.LOCKED, AuthUserStatus.DISABLED);
    }

    @Test
    @DisplayName("getAllowedTransitions - DISABLED")
    void getAllowedTransitions_Disabled() {
        assertThat(AuthUserStatus.DISABLED.getAllowedTransitions())
                .containsExactly(AuthUserStatus.ACTIVE);
    }
}