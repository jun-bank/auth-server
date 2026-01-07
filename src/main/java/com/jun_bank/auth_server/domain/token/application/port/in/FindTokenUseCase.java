package com.jun_bank.auth_server.domain.token.application.port.in;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

/**
 * 토큰 조회 UseCase (Input Port)
 *
 * RefreshToken 조회를 담당합니다.
 * Auth 도메인에서 토큰 갱신, 검증 시 호출합니다.
 */
public interface FindTokenUseCase {

    /**
     * ID로 토큰 조회
     *
     * @param refreshTokenId RefreshToken ID (RTK-xxx)
     * @return Optional RefreshToken
     */
    Optional<RefreshToken> findById(String refreshTokenId);

    /**
     * 토큰 값으로 조회
     *
     * @param token JWT Refresh Token 값
     * @return Optional RefreshToken
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 유효한 토큰만 조회 (만료/폐기 확인)
     *
     * @param token JWT Refresh Token 값
     * @return Optional RefreshToken (유효한 경우만)
     */
    Optional<RefreshToken> findValidToken(String token);

    /**
     * 사용자의 유효한 토큰 목록 조회
     *
     * @param userId User Service의 사용자 ID (USR-xxx)
     * @return 유효한 RefreshToken 목록
     */
    List<RefreshToken> findValidTokensByUserId(String userId);

    /**
     * 사용자의 유효한 토큰 수 조회
     *
     * @param userId User Service의 사용자 ID
     * @return 유효한 토큰 수
     */
    long countValidTokensByUserId(String userId);

    /**
     * 토큰 존재 여부 확인
     *
     * @param token JWT Refresh Token 값
     * @return 존재하면 true
     */
    boolean existsByToken(String token);
}