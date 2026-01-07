package com.jun_bank.auth_server.domain.token.application.port.in;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;

/**
 * 토큰 저장 UseCase (Input Port)
 *
 * RefreshToken 저장을 담당합니다.
 * Auth 도메인에서 로그인 성공 시 호출합니다.
 */
public interface SaveTokenUseCase {

    /**
     * RefreshToken 저장
     *
     * @param refreshToken 저장할 토큰
     * @return 저장된 토큰 (ID 포함)
     */
    RefreshToken save(RefreshToken refreshToken);

    /**
     * RefreshToken 저장 (최대 세션 수 지정)
     *
     * 최대 세션 수를 초과하면 가장 오래된 토큰이 삭제됩니다.
     *
     * @param refreshToken 저장할 토큰
     * @param maxSessions  최대 허용 세션 수
     * @return 저장된 토큰 (ID 포함)
     */
    RefreshToken save(RefreshToken refreshToken, int maxSessions);
}