package com.jun_bank.auth_server.domain.history.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.history.domain.model.LoginHistory;
import com.jun_bank.auth_server.domain.history.domain.model.vo.LoginHistoryId;
import com.jun_bank.auth_server.domain.history.infrastructure.persistence.entity.LoginHistoryEntity;
import org.springframework.stereotype.Component;

/**
 * LoginHistory Entity ↔ Domain 변환 매퍼
 * <p>
 * JPA Entity와 도메인 모델 간의 변환을 담당합니다.
 *
 * <h3>메서드 역할:</h3>
 * <ul>
 *   <li>{@link #toEntity(LoginHistory)}: 신규 저장용 (ID 생성 포함)</li>
 *   <li>{@link #toDomain(LoginHistoryEntity)}: 조회 결과 → 도메인 복원</li>
 * </ul>
 *
 * <h3>특이사항:</h3>
 * <p>
 * LoginHistory는 Append-only이므로 updateEntity 메서드가 없습니다.
 * </p>
 */
@Component
public class LoginHistoryMapper {

    /**
     * 도메인 모델을 JPA 엔티티로 변환 (신규 저장용)
     * <p>
     * 신규 이력(loginHistoryId가 null)인 경우 새 ID를 생성합니다.
     * </p>
     *
     * @param domain LoginHistory 도메인 모델
     * @return LoginHistoryEntity (신규 ID 포함)
     */
    public LoginHistoryEntity toEntity(LoginHistory domain) {
        // 신규 이력인 경우 ID 생성
        String loginHistoryId = domain.isNew()
                ? LoginHistoryId.generateId()
                : domain.getLoginHistoryId().value();

        if (domain.isSuccess()) {
            return LoginHistoryEntity.success(
                    loginHistoryId,
                    domain.getUserId(),
                    domain.getEmail(),
                    domain.getLoginAt(),
                    domain.getIpAddress(),
                    domain.getUserAgent()
            );
        } else {
            return LoginHistoryEntity.failure(
                    loginHistoryId,
                    domain.getUserId(),
                    domain.getEmail(),
                    domain.getLoginAt(),
                    domain.getIpAddress(),
                    domain.getUserAgent(),
                    domain.getFailReason()
            );
        }
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity LoginHistoryEntity
     * @return LoginHistory 도메인 모델
     */
    public LoginHistory toDomain(LoginHistoryEntity entity) {
        return LoginHistory.restoreBuilder()
                .loginHistoryId(LoginHistoryId.of(entity.getLoginHistoryId()))
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .loginAt(entity.getLoginAt())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .success(entity.isSuccess())
                .failReason(entity.getFailReason())
                .build();
    }

    // ========================================
    // Append-only: updateEntity 메서드 없음
    // ========================================
    // LoginHistory는 생성 후 수정되지 않습니다.
}