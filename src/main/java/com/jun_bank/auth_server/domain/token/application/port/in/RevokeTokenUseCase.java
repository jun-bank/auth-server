package com.jun_bank.auth_server.domain.token.application.port.in;

/**
 * 토큰 폐기 UseCase (Input Port)
 *
 * RefreshToken 폐기를 담당합니다.
 * Auth 도메인에서 로그아웃, 비밀번호 변경, 회원탈퇴 시 호출합니다.
 */
public interface RevokeTokenUseCase {

    /**
     * 토큰 값으로 폐기
     *
     * 단일 로그아웃 시 사용합니다.
     *
     * @param token JWT Refresh Token 값
     */
    void revokeByToken(String token);

    /**
     * 사용자의 모든 토큰 폐기
     *
     * 전체 로그아웃, 비밀번호 변경, 회원탈퇴 시 사용합니다.
     *
     * @param userId User Service의 사용자 ID (USR-xxx)
     * @return 폐기된 토큰 수
     */
    int revokeAllByUserId(String userId);

    /**
     * 세션 ID로 폐기 (권한 검증 포함)
     *
     * 마이페이지에서 특정 기기 로그아웃 시 사용합니다.
     *
     * @param userId    요청자의 사용자 ID (권한 검증용)
     * @param sessionId 종료할 세션 ID (RTK-xxx)
     * @throws com.jun_bank.auth_server.domain.token.domain.exception.TokenException
     *         세션을 찾을 수 없거나 권한이 없는 경우
     */
    void revokeBySessionId(String userId, String sessionId);

    /**
     * 다른 세션 전체 폐기 (현재 세션 제외)
     *
     * "다른 기기 모두 로그아웃" 기능에 사용합니다.
     *
     * @param userId         사용자 ID
     * @param currentTokenId 유지할 현재 세션의 토큰 ID
     * @return 폐기된 세션 수
     */
    int revokeOtherSessions(String userId, String currentTokenId);
}