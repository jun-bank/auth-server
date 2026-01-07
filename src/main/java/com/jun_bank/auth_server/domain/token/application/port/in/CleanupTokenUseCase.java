package com.jun_bank.auth_server.domain.token.application.port.in;

/**
 * 토큰 정리 UseCase (Input Port)
 *
 * 만료/폐기된 토큰 정리를 담당합니다.
 * 배치 스케줄러에서 주기적으로 호출합니다.
 */
public interface CleanupTokenUseCase {

    /**
     * 만료된 토큰 삭제
     *
     * @return 삭제된 토큰 수
     */
    int deleteExpiredTokens();

    /**
     * 폐기된 오래된 토큰 삭제
     *
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 토큰 수
     */
    int deleteOldRevokedTokens(int retentionDays);
}