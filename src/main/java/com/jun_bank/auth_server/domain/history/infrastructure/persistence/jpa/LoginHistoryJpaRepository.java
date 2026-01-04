package com.jun_bank.auth_server.domain.history.infrastructure.persistence.jpa;

import com.jun_bank.auth_server.domain.history.infrastructure.persistence.entity.LoginHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LoginHistory JPA Repository
 * <p>
 * 로그인 이력 저장 및 조회를 담당합니다.
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li><b>Append-only</b>: save()와 조회만 사용</li>
 *   <li>update/delete 메서드 제공 안함</li>
 *   <li>배치 삭제만 예외적으로 허용 (법적 보관 기간 후)</li>
 * </ul>
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>사용자별 이력 조회</li>
 *   <li>IP별 이력 조회 (보안 감사)</li>
 *   <li>기간별 이력 조회</li>
 *   <li>실패 패턴 분석</li>
 * </ul>
 */
public interface LoginHistoryJpaRepository extends JpaRepository<LoginHistoryEntity, String> {

    // ========================================
    // 사용자별 조회
    // ========================================

    /**
     * 사용자의 로그인 이력 조회 (최신순)
     *
     * @param userId   User Service의 사용자 ID
     * @param pageable 페이징
     * @return Page<LoginHistoryEntity>
     */
    Page<LoginHistoryEntity> findByUserIdOrderByLoginAtDesc(String userId, Pageable pageable);

    /**
     * 사용자의 최근 로그인 이력 조회 (제한)
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistoryEntity>
     */
    @Query("SELECT h FROM LoginHistoryEntity h WHERE h.userId = :userId " +
            "ORDER BY h.loginAt DESC LIMIT :limit")
    List<LoginHistoryEntity> findRecentByUserId(
            @Param("userId") String userId,
            @Param("limit") int limit);

    /**
     * 사용자의 최근 로그인 성공 이력
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistoryEntity>
     */
    @Query("SELECT h FROM LoginHistoryEntity h WHERE h.userId = :userId " +
            "AND h.success = true ORDER BY h.loginAt DESC LIMIT :limit")
    List<LoginHistoryEntity> findRecentSuccessByUserId(
            @Param("userId") String userId,
            @Param("limit") int limit);

    /**
     * 사용자의 최근 로그인 실패 이력
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistoryEntity>
     */
    @Query("SELECT h FROM LoginHistoryEntity h WHERE h.userId = :userId " +
            "AND h.success = false ORDER BY h.loginAt DESC LIMIT :limit")
    List<LoginHistoryEntity> findRecentFailuresByUserId(
            @Param("userId") String userId,
            @Param("limit") int limit);

    // ========================================
    // 이메일별 조회 (사용자 찾기 실패 포함)
    // ========================================

    /**
     * 이메일로 로그인 이력 조회
     * <p>
     * 사용자가 없는 경우(USER_NOT_FOUND)도 포함합니다.
     * </p>
     *
     * @param email    이메일
     * @param pageable 페이징
     * @return Page<LoginHistoryEntity>
     */
    Page<LoginHistoryEntity> findByEmailOrderByLoginAtDesc(String email, Pageable pageable);

    // ========================================
    // IP별 조회 (보안 감사)
    // ========================================

    /**
     * 특정 IP의 로그인 시도 이력
     * <p>
     * 의심스러운 IP 활동 분석에 사용합니다.
     * </p>
     *
     * @param ipAddress 접속 IP
     * @param from      시작 시간
     * @param to        종료 시간
     * @return List<LoginHistoryEntity>
     */
    @Query("SELECT h FROM LoginHistoryEntity h WHERE h.ipAddress = :ip " +
            "AND h.loginAt BETWEEN :from AND :to ORDER BY h.loginAt DESC")
    List<LoginHistoryEntity> findByIpAddressAndPeriod(
            @Param("ip") String ipAddress,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 특정 IP의 실패 횟수 조회 (기간 내)
     * <p>
     * 무차별 대입 공격 탐지에 사용합니다.
     * </p>
     *
     * @param ipAddress 접속 IP
     * @param from      시작 시간
     * @return 실패 횟수
     */
    @Query("SELECT COUNT(h) FROM LoginHistoryEntity h WHERE h.ipAddress = :ip " +
            "AND h.success = false AND h.loginAt >= :from")
    long countFailuresByIpAddressSince(
            @Param("ip") String ipAddress,
            @Param("from") LocalDateTime from);

    // ========================================
    // 기간별 조회
    // ========================================

    /**
     * 기간 내 로그인 이력 조회
     *
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이징
     * @return Page<LoginHistoryEntity>
     */
    @Query("SELECT h FROM LoginHistoryEntity h WHERE h.loginAt BETWEEN :from AND :to " +
            "ORDER BY h.loginAt DESC")
    Page<LoginHistoryEntity> findByPeriod(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    // ========================================
    // 통계
    // ========================================

    /**
     * 기간 내 성공/실패 횟수 조회
     *
     * @param success 성공 여부
     * @param from    시작 시간
     * @param to      종료 시간
     * @return 횟수
     */
    long countBySuccessAndLoginAtBetween(boolean success, LocalDateTime from, LocalDateTime to);

    /**
     * 특정 실패 사유 횟수 조회
     *
     * @param failReason 실패 사유
     * @param from       시작 시간
     * @param to         종료 시간
     * @return 횟수
     */
    long countByFailReasonAndLoginAtBetween(String failReason, LocalDateTime from, LocalDateTime to);

    // ========================================
    // 배치 삭제 (법적 보관 기간 후)
    // ========================================

    /**
     * 오래된 로그인 이력 삭제
     * <p>
     * 법적 보관 기간(예: 3년) 경과 후 배치에서 사용합니다.
     * </p>
     *
     * @param threshold 기준 시간
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM LoginHistoryEntity h WHERE h.loginAt < :threshold")
    int deleteOldHistories(@Param("threshold") LocalDateTime threshold);
}