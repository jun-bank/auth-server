package com.jun_bank.auth_server.domain.token.infrastructure.persistence.jpa;

import com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RefreshToken JPA Repository
 * <p>
 * Spring Data JPA 기반의 리프레시 토큰 저장소입니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>토큰 값 또는 ID로 단건 조회</li>
 *   <li>사용자별 토큰 목록 조회</li>
 *   <li>만료/폐기 토큰 정리</li>
 * </ul>
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {

    // ========================================
    // 단건 조회
    // ========================================

    /**
     * 토큰 값으로 조회
     * <p>
     * 토큰 갱신 요청 시 사용합니다.
     * </p>
     *
     * @param token JWT 토큰 값
     * @return Optional<RefreshTokenEntity>
     */
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * 유효한 토큰 조회 (폐기되지 않고 만료되지 않은)
     *
     * @param token JWT 토큰 값
     * @param now   현재 시간
     * @return Optional<RefreshTokenEntity>
     */
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.token = :token " +
            "AND r.isRevoked = false AND r.expiresAt > :now")
    Optional<RefreshTokenEntity> findValidToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now);

    // ========================================
    // 사용자별 조회
    // ========================================

    /**
     * 사용자의 모든 토큰 조회
     *
     * @param userId User Service의 사용자 ID
     * @return List<RefreshTokenEntity>
     */
    List<RefreshTokenEntity> findByUserId(String userId);

    /**
     * 사용자의 유효한 토큰 목록 조회
     * <p>
     * 동시 로그인 세션 관리에 사용합니다.
     * </p>
     *
     * @param userId User Service의 사용자 ID
     * @param now    현재 시간
     * @return List<RefreshTokenEntity>
     */
    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.userId = :userId " +
            "AND r.isRevoked = false AND r.expiresAt > :now " +
            "ORDER BY r.createdAt DESC")
    List<RefreshTokenEntity> findValidTokensByUserId(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now);

    /**
     * 사용자의 유효한 토큰 수 조회
     *
     * @param userId User Service의 사용자 ID
     * @param now    현재 시간
     * @return 유효한 토큰 수
     */
    @Query("SELECT COUNT(r) FROM RefreshTokenEntity r WHERE r.userId = :userId " +
            "AND r.isRevoked = false AND r.expiresAt > :now")
    long countValidTokensByUserId(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now);

    // ========================================
    // 토큰 폐기
    // ========================================

    /**
     * 사용자의 모든 토큰 폐기
     * <p>
     * 로그아웃(전체) 또는 비밀번호 변경 시 사용합니다.
     * </p>
     *
     * @param userId User Service의 사용자 ID
     * @return 폐기된 토큰 수
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.isRevoked = true WHERE r.userId = :userId")
    int revokeAllByUserId(@Param("userId") String userId);

    /**
     * 특정 토큰 폐기 (토큰 값으로)
     *
     * @param token JWT 토큰 값
     * @return 폐기된 토큰 수
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.isRevoked = true WHERE r.token = :token")
    int revokeByToken(@Param("token") String token);

    // ========================================
    // 정리 (배치용)
    // ========================================

    /**
     * 만료된 토큰 삭제
     * <p>
     * 배치에서 오래된 토큰 정리에 사용합니다.
     * </p>
     *
     * @param threshold 기준 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :threshold")
    int deleteExpiredTokens(@Param("threshold") LocalDateTime threshold);

    /**
     * 폐기된 오래된 토큰 삭제
     * <p>
     * 폐기 후 일정 시간이 지난 토큰을 정리합니다.
     * </p>
     *
     * @param threshold 기준 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.isRevoked = true AND r.createdAt < :threshold")
    int deleteOldRevokedTokens(@Param("threshold") LocalDateTime threshold);

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