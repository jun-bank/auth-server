package com.jun_bank.auth_server.domain.token.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;
import com.jun_bank.auth_server.domain.token.domain.model.vo.RefreshTokenId;
import com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

/**
 * RefreshToken Entity ↔ Domain 변환 매퍼
 * <p>
 * JPA Entity와 도메인 모델 간의 변환을 담당합니다.
 *
 * <h3>메서드 역할:</h3>
 * <ul>
 *   <li>{@link #toEntity(RefreshToken)}: 신규 저장용 (ID 생성 포함)</li>
 *   <li>{@link #toDomain(RefreshTokenEntity)}: 조회 결과 → 도메인 복원</li>
 * </ul>
 *
 * <h3>특이사항:</h3>
 * <p>
 * RefreshToken은 생성 후 수정하지 않고 폐기만 하므로
 * updateEntity 메서드가 없습니다.
 * </p>
 */
@Component
public class RefreshTokenMapper {

    /**
     * 도메인 모델을 JPA 엔티티로 변환 (신규 저장용)
     * <p>
     * 신규 토큰(refreshTokenId가 null)인 경우 새 ID를 생성합니다.
     * </p>
     *
     * @param domain RefreshToken 도메인 모델
     * @return RefreshTokenEntity (신규 ID 포함)
     */
    public RefreshTokenEntity toEntity(RefreshToken domain) {
        // 신규 토큰인 경우 ID 생성
        String refreshTokenId = domain.isNew()
                ? RefreshTokenId.generateId()
                : domain.getRefreshTokenId().value();

        return RefreshTokenEntity.of(
                refreshTokenId,
                domain.getUserId(),
                domain.getToken(),
                domain.getExpiresAt(),
                domain.getDeviceInfo(),
                domain.getIpAddress()
        );
    }

    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity RefreshTokenEntity
     * @return RefreshToken 도메인 모델
     */
    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.restoreBuilder()
                .refreshTokenId(RefreshTokenId.of(entity.getRefreshTokenId()))
                .userId(entity.getUserId())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .isRevoked(entity.isRevoked())
                .deviceInfo(entity.getDeviceInfo())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 토큰 폐기 (Entity에 직접 적용)
     * <p>
     * 도메인에서 revoke() 호출 후 이 메서드로 Entity에 반영합니다.
     * </p>
     *
     * @param entity 폐기할 엔티티
     */
    public void revokeEntity(RefreshTokenEntity entity) {
        entity.revoke();
    }
}