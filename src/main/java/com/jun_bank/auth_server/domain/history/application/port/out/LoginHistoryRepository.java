package com.jun_bank.auth_server.domain.history.application.port.out;

import com.jun_bank.auth_server.domain.history.domain.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * LoginHistory Repository Port (Output Port)
 * <p>
 * Application Layer에서 사용하는 Repository 인터페이스입니다.
 * Infrastructure Layer의 구현체와 분리하여 의존성 역전을 실현합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>도메인 모델(LoginHistory)만 다룸 - Entity 노출 안함</li>
 *   <li>Infrastructure 의존성 없음</li>
 *   <li>비즈니스 관점의 메서드명 사용</li>
 * </ul>
 *
 * <h3>메서드 분류:</h3>
 * <ul>
 *   <li>저장: 이력 기록 (Append-only)</li>
 *   <li>조회: 사용자별, IP별, 기간별</li>
 *   <li>통계: 성공/실패 횟수</li>
 *   <li>정리: 오래된 이력 삭제 (배치)</li>
 * </ul>
 *
 * <h3>특이사항:</h3>
 * <p>
 * LoginHistory는 <b>Append-only</b>입니다.
 * update 메서드가 없습니다.
 * </p>
 */
public interface LoginHistoryRepository {

    // ========================================
    // 저장 (Append-only)
    // ========================================

    /**
     * 로그인 이력 저장
     * <p>
     * 신규 이력을 추가합니다. ID는 Mapper에서 자동 생성됩니다.
     * 기존 이력은 수정되지 않습니다.
     * </p>
     *
     * @param loginHistory 저장할 이력
     * @return 저장된 이력 (ID 포함)
     */
    LoginHistory save(LoginHistory loginHistory);

    // ========================================
    // 단건 조회
    // ========================================

    /**
     * ID로 이력 조회
     *
     * @param loginHistoryId 로그인 이력 ID (LGH-xxx)
     * @return Optional<LoginHistory>
     */
    Optional<LoginHistory> findById(String loginHistoryId);

    // ========================================
    // 사용자별 조회
    // ========================================

    /**
     * 사용자의 로그인 이력 조회 (페이징)
     *
     * @param userId   User Service의 사용자 ID
     * @param pageable 페이징
     * @return Page<LoginHistory>
     */
    Page<LoginHistory> findByUserId(String userId, Pageable pageable);

    /**
     * 사용자의 최근 이력 조회 (제한)
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistory>
     */
    List<LoginHistory> findRecentByUserId(String userId, int limit);

    /**
     * 사용자의 최근 성공 이력
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistory>
     */
    List<LoginHistory> findRecentSuccessByUserId(String userId, int limit);

    /**
     * 사용자의 최근 실패 이력
     *
     * @param userId User Service의 사용자 ID
     * @param limit  최대 개수
     * @return List<LoginHistory>
     */
    List<LoginHistory> findRecentFailuresByUserId(String userId, int limit);

    // ========================================
    // 이메일별 조회
    // ========================================

    /**
     * 이메일로 로그인 이력 조회
     * <p>
     * 사용자가 없는 경우(USER_NOT_FOUND)도 포함합니다.
     * </p>
     *
     * @param email    이메일
     * @param pageable 페이징
     * @return Page<LoginHistory>
     */
    Page<LoginHistory> findByEmail(String email, Pageable pageable);

    // ========================================
    // IP별 조회 (보안 감사)
    // ========================================

    /**
     * 특정 IP의 로그인 이력
     * <p>
     * 의심스러운 IP 활동 분석에 사용합니다.
     * </p>
     *
     * @param ipAddress 접속 IP
     * @param from      시작 시간
     * @param to        종료 시간
     * @return List<LoginHistory>
     */
    List<LoginHistory> findByIpAddressAndPeriod(String ipAddress, LocalDateTime from, LocalDateTime to);

    /**
     * 특정 IP의 실패 횟수 (기간 내)
     * <p>
     * 무차별 대입 공격 탐지에 사용합니다.
     * </p>
     *
     * @param ipAddress    접속 IP
     * @param sinceMinutes 조회 기간 (분)
     * @return 실패 횟수
     */
    long countFailuresByIpAddressSince(String ipAddress, int sinceMinutes);

    // ========================================
    // 기간별 조회
    // ========================================

    /**
     * 기간 내 로그인 이력 조회
     *
     * @param from     시작 시간
     * @param to       종료 시간
     * @param pageable 페이징
     * @return Page<LoginHistory>
     */
    Page<LoginHistory> findByPeriod(LocalDateTime from, LocalDateTime to, Pageable pageable);

    // ========================================
    // 통계
    // ========================================

    /**
     * 기간 내 성공 횟수
     *
     * @param from 시작 시간
     * @param to   종료 시간
     * @return 성공 횟수
     */
    long countSuccessByPeriod(LocalDateTime from, LocalDateTime to);

    /**
     * 기간 내 실패 횟수
     *
     * @param from 시작 시간
     * @param to   종료 시간
     * @return 실패 횟수
     */
    long countFailuresByPeriod(LocalDateTime from, LocalDateTime to);

    /**
     * 특정 실패 사유 횟수
     *
     * @param failReason 실패 사유
     * @param from       시작 시간
     * @param to         종료 시간
     * @return 횟수
     */
    long countByFailReasonAndPeriod(String failReason, LocalDateTime from, LocalDateTime to);

    // ========================================
    // 배치 삭제 (법적 보관 기간 후)
    // ========================================

    /**
     * 오래된 로그인 이력 삭제
     * <p>
     * 법적 보관 기간 경과 후 배치에서 사용합니다.
     * </p>
     *
     * @param retentionYears 보관 기간 (년)
     * @return 삭제된 행 수
     */
    int deleteOldHistories(int retentionYears);
}