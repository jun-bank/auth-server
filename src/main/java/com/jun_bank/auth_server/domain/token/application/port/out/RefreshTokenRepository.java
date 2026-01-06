package com.jun_bank.auth_server.domain.token.application.port.out;

import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

/**
 * RefreshToken Repository Port (Output Port)
 * <p>
 * Application Layer에서 사용하는 Repository 인터페이스입니다.
 * Infrastructure Layer의 구현체와 분리하여 의존성 역전을 실현합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>도메인 모델(RefreshToken)만 다룸 - Entity 노출 안함</li>
 *   <li>Infrastructure 의존성 없음</li>
 *   <li>비즈니스 관점의 메서드명 사용</li>
 * </ul>
 *
 * <h3>메서드 분류:</h3>
 * <ul>
 *   <li>저장: 신규 토큰 저장</li>
 *   <li>조회: ID, 토큰 값, 사용자별 조회</li>
 *   <li>폐기: 단건/전체 폐기</li>
 *   <li>정리: 만료된 토큰 삭제 (배치)</li>
 * </ul>
 *
 * <h3>특이사항:</h3>
 * <p>
 * RefreshToken은 생성 후 수정하지 않고 폐기만 합니다.
 * 따라서 update 메서드가 없습니다.
 * </p>
 */
public interface RefreshTokenRepository {

    // ========================================
    // 저장
    // ========================================

    /**
     * 리프레시 토큰 저장
     * <p>
     * 신규 토큰을 저장합니다. ID는 Mapper에서 자동 생성됩니다.
     * </p>
     *
     * @param refreshToken 저장할 토큰
     * @return 저장된 토큰 (ID 포함)
     */
    RefreshToken save(RefreshToken refreshToken);

    /**
     * 리프레시 토큰 저장 (최대 세션 수 지정)
     * <p>
     * 최대 세션 수를 초과하면 가장 오래된 토큰이 삭제됩니다.
     * </p>
     *
     * @param refreshToken 저장할 토큰
     * @param maxSessions  최대 허용 세션 수
     * @return 저장된 토큰 (ID 포함)
     */
    RefreshToken save(RefreshToken refreshToken, int maxSessions);

    // ========================================
    // 단건 조회
    // ========================================

    /**
     * ID로 토큰 조회
     *
     * @param refreshTokenId 리프레시 토큰 ID (RTK-xxx)
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findById(String refreshTokenId);

    /**
     * 토큰 값으로 조회
     * <p>
     * 토큰 갱신 요청 시 사용합니다.
     * </p>
     *
     * @param token JWT 토큰 값
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 유효한 토큰 조회
     * <p>
     * 폐기되지 않고 만료되지 않은 토큰만 반환합니다.
     * </p>
     *
     * @param token JWT 토큰 값
     * @return Optional<RefreshToken> (유효한 경우만)
     */
    Optional<RefreshToken> findValidToken(String token);

    // ========================================
    // 사용자별 조회
    // ========================================

    /**
     * 사용자의 모든 토큰 조회
     *
     * @param userId User Service의 사용자 ID
     * @return List<RefreshToken>
     */
    List<RefreshToken> findByUserId(String userId);

    /**
     * 사용자의 유효한 토큰 목록 조회
     * <p>
     * 동시 로그인 세션 관리에 사용합니다.
     * </p>
     *
     * @param userId User Service의 사용자 ID
     * @return List<RefreshToken>
     */
    List<RefreshToken> findValidTokensByUserId(String userId);

    /**
     * 사용자의 유효한 토큰 수 조회
     *
     * @param userId User Service의 사용자 ID
     * @return 유효한 토큰 수
     */
    long countValidTokensByUserId(String userId);

    // ========================================
    // 토큰 폐기
    // ========================================

    /**
     * 특정 토큰 폐기 (토큰 값으로)
     * <p>
     * 로그아웃(단건) 시 사용합니다.
     * </p>
     *
     * @param token JWT 토큰 값
     */
    void revokeByToken(String token);

    /**
     * 사용자의 모든 토큰 폐기
     * <p>
     * 전체 로그아웃 또는 비밀번호 변경 시 사용합니다.
     * </p>
     *
     * @param userId User Service의 사용자 ID
     * @return 폐기된 토큰 수
     */
    int revokeAllByUserId(String userId);

    // ========================================
    // 정리 (배치용)
    // ========================================

    /**
     * 만료된 토큰 삭제
     * <p>
     * 배치에서 오래된 토큰 정리에 사용합니다.
     * </p>
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

    // ========================================
    // 존재 여부 확인
    // ========================================

    /**
     * 토큰 존재 여부 확인
     *
     * @param token JWT 토큰 값
     * @return 존재하면 true
     */
    boolean existsByToken(String token);
}