package com.jun_bank.auth_server.domain.token.application.port.in;

import com.jun_bank.auth_server.domain.token.application.dto.result.SessionResult;

import java.util.List;

/**
 * 세션 조회 UseCase (Input Port)
 *
 * 마이페이지 "로그인된 기기 관리" 기능을 위한 세션 조회를 담당합니다.
 * Presentation Layer에서 직접 호출합니다.
 *
 * @see SessionResult
 */
public interface GetSessionUseCase {

    /**
     * 사용자의 활성 세션 목록 조회
     *
     * @param userId         User Service의 사용자 ID (USR-xxx)
     * @param currentTokenId 현재 요청의 토큰 ID (현재 세션 표시용, nullable)
     * @return 활성 세션 목록 (SessionResult DTO)
     */
    List<SessionResult> getActiveSessions(String userId, String currentTokenId);

    /**
     * 사용자의 활성 세션 수 조회
     *
     * @param userId User Service의 사용자 ID
     * @return 활성 세션 수
     */
    long countActiveSessions(String userId);
}