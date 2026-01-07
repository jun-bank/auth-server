package com.jun_bank.auth_server.domain.token.application.dto.result;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;

import java.time.LocalDateTime;

/**
 * 세션 정보 Result DTO
 *
 * 사용자의 활성 세션(RefreshToken) 정보를 반환합니다.
 * 마이페이지 "로그인된 기기 관리" 기능에서 사용됩니다.
 *
 * 활용:
 * - 현재 로그인된 기기 목록 조회
 * - 특정 세션 강제 종료
 * - 의심스러운 세션 탐지
 *
 * @param sessionId        세션 ID (RefreshToken ID: RTK-xxx)
 * @param deviceInfo       디바이스 정보 (User-Agent)
 * @param ipAddress        접속 IP
 * @param createdAt        세션 생성 시간
 * @param expiresAt        세션 만료 시간
 * @param isCurrentSession 현재 요청의 세션 여부
 */
public record SessionResult(
        String sessionId,
        String deviceInfo,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean isCurrentSession
) {

    /**
     * RefreshToken 도메인에서 SessionResult 생성
     *
     * @param refreshToken   RefreshToken 도메인 모델
     * @param currentTokenId 현재 요청의 토큰 ID (현재 세션 판별용)
     * @return SessionResult
     */
    public static SessionResult from(RefreshToken refreshToken, String currentTokenId) {
        String tokenId = refreshToken.getRefreshTokenId() != null
                ? refreshToken.getRefreshTokenId().value()
                : null;

        boolean isCurrent = tokenId != null && tokenId.equals(currentTokenId);

        return new SessionResult(
                tokenId,
                refreshToken.getDeviceInfo(),
                refreshToken.getIpAddress(),
                refreshToken.getCreatedAt(),
                refreshToken.getExpiresAt(),
                isCurrent
        );
    }

    /**
     * RefreshToken 도메인에서 SessionResult 생성 (현재 세션 아님)
     *
     * @param refreshToken RefreshToken 도메인 모델
     * @return SessionResult
     */
    public static SessionResult from(RefreshToken refreshToken) {
        return from(refreshToken, null);
    }
}