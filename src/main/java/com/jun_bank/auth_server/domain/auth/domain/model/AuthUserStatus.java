package com.jun_bank.auth_server.domain.auth.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 인증 사용자 상태
 * <p>
 * 인증 관점에서의 사용자 상태를 정의합니다.
 * User Service의 {@code UserStatus}와는 별개로 Auth Server에서 관리합니다.
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 *                   로그인 실패 횟수 초과
 * ┌────────┐ ─────────────────────────────▶ ┌────────┐
 * │ ACTIVE │                                 │ LOCKED │
 * └────────┘ ◀───────────────────────────── └────────┘
 *     │            잠금 시간 경과 / 수동 해제      │
 *     │                                        │
 *     │ 관리자 비활성화                관리자 비활성화
 *     ▼                                        ▼
 * ┌──────────┐                           ┌──────────┐
 * │ DISABLED │ ◀─────────────────────────│ DISABLED │
 * └──────────┘     (LOCKED에서 직접 전환 가능)
 *     │
 *     │ 관리자 활성화
 *     ▼
 * ┌────────┐
 * │ ACTIVE │
 * └────────┘
 * </pre>
 *
 * <h3>상태별 특성:</h3>
 * <table border="1">
 *   <tr><th>상태</th><th>인증 가능</th><th>설명</th></tr>
 *   <tr><td>ACTIVE</td><td>✓</td><td>정상 상태</td></tr>
 *   <tr><td>LOCKED</td><td>✗ (시간 경과 시 ✓)</td><td>일시적 잠금</td></tr>
 *   <tr><td>DISABLED</td><td>✗</td><td>영구 비활성화</td></tr>
 * </table>
 *
 * @see AuthUser
 */
public enum AuthUserStatus {

    /**
     * 정상 상태
     * <p>
     * 로그인 및 모든 인증 기능을 정상적으로 사용할 수 있는 상태입니다.
     * </p>
     */
    ACTIVE("정상", true),

    /**
     * 잠금 상태
     * <p>
     * 로그인 실패 횟수 초과로 인한 일시적 잠금 상태입니다.
     * {@code lockedUntil} 시간이 경과하면 자동으로 로그인이 가능해집니다.
     * 관리자가 수동으로 해제할 수도 있습니다.
     * </p>
     *
     * <h4>잠금 정책:</h4>
     * <ul>
     *   <li>기본 최대 시도 횟수: 5회</li>
     *   <li>기본 잠금 시간: 30분</li>
     * </ul>
     */
    LOCKED("잠금", false),

    /**
     * 비활성화 상태
     * <p>
     * 관리자에 의해 영구적으로 비활성화된 상태입니다.
     * 이용약관 위반, 사기 행위 등의 사유로 비활성화됩니다.
     * 관리자만 상태를 ACTIVE로 변경할 수 있습니다.
     * </p>
     */
    DISABLED("비활성화", false);

    private final String description;
    private final boolean canAuthenticate;

    /**
     * AuthUserStatus 생성자
     *
     * @param description 상태 설명
     * @param canAuthenticate 인증 가능 여부
     */
    AuthUserStatus(String description, boolean canAuthenticate) {
        this.description = description;
        this.canAuthenticate = canAuthenticate;
    }

    /**
     * 상태 설명 반환
     *
     * @return 한글 상태 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 인증(로그인) 가능 여부 확인
     * <p>
     * <strong>주의:</strong> LOCKED 상태의 경우 이 메서드는 false를 반환하지만,
     * 잠금 시간이 경과한 경우에는 로그인이 가능할 수 있습니다.
     * 실제 로그인 가능 여부는 {@link AuthUser#canLogin()}을 사용하세요.
     * </p>
     *
     * @return 기본 인증 가능 상태이면 true
     */
    public boolean canAuthenticate() {
        return canAuthenticate;
    }

    /**
     * 활성 상태 여부 확인
     *
     * @return ACTIVE이면 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 잠금 상태 여부 확인
     *
     * @return LOCKED이면 true
     */
    public boolean isLocked() {
        return this == LOCKED;
    }

    /**
     * 비활성화 상태 여부 확인
     *
     * @return DISABLED이면 true
     */
    public boolean isDisabled() {
        return this == DISABLED;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     * <p>
     * 같은 상태로의 전환은 불가능합니다.
     * 상태 전이 규칙에 따라 허용된 전환인지 검증합니다.
     * </p>
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(AuthUserStatus target) {
        if (this == target) {
            return false;
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     * <p>
     * 상태 전이 규칙에 따라 허용된 상태들의 Set을 반환합니다.
     * </p>
     *
     * @return 전환 가능한 상태 Set
     */
    public Set<AuthUserStatus> getAllowedTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(LOCKED, DISABLED);
            case LOCKED -> EnumSet.of(ACTIVE, DISABLED);
            case DISABLED -> EnumSet.of(ACTIVE);
        };
    }
}