package com.jun_bank.auth_server.domain.history.infrastructure.persistence;

import com.jun_bank.auth_server.domain.history.application.port.out.LoginHistoryRepository;
import com.jun_bank.auth_server.domain.history.domain.model.LoginHistory;
import com.jun_bank.auth_server.domain.history.infrastructure.persistence.entity.LoginHistoryEntity;
import com.jun_bank.auth_server.domain.history.infrastructure.persistence.jpa.LoginHistoryJpaRepository;
import com.jun_bank.auth_server.domain.history.infrastructure.persistence.mapper.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * LoginHistory Repository Adapter (Output Adapter)
 * <p>
 * Application Layer의 {@link LoginHistoryRepository} Port를 구현합니다.
 * JPA Repository를 조합하여 영속성 로직을 처리합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>Domain ↔ Entity 변환 (Mapper 사용)</li>
 *   <li>JPA Repository 호출</li>
 *   <li>트랜잭션 관리</li>
 * </ul>
 *
 * <h3>특이사항:</h3>
 * <p>
 * LoginHistory는 <b>Append-only</b>이므로 저장과 조회만 지원합니다.
 * update 메서드가 없습니다.
 * </p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginHistoryRepositoryAdapter implements LoginHistoryRepository {

    private final LoginHistoryJpaRepository loginHistoryJpaRepository;
    private final LoginHistoryMapper loginHistoryMapper;

    // ========================================
    // 저장 (Append-only)
    // ========================================

    @Override
    @Transactional
    public LoginHistory save(LoginHistory loginHistory) {
        LoginHistoryEntity entity = loginHistoryMapper.toEntity(loginHistory);
        entity = loginHistoryJpaRepository.save(entity);

        log.debug("LoginHistory 저장 완료: email={}, success={}",
                loginHistory.getEmail(), loginHistory.isSuccess());
        return loginHistoryMapper.toDomain(entity);
    }

    // ========================================
    // 단건 조회
    // ========================================

    @Override
    public Optional<LoginHistory> findById(String loginHistoryId) {
        return loginHistoryJpaRepository.findById(loginHistoryId)
                .map(loginHistoryMapper::toDomain);
    }

    // ========================================
    // 사용자별 조회
    // ========================================

    @Override
    public Page<LoginHistory> findByUserId(String userId, Pageable pageable) {
        return loginHistoryJpaRepository.findByUserIdOrderByLoginAtDesc(userId, pageable)
                .map(loginHistoryMapper::toDomain);
    }

    @Override
    public List<LoginHistory> findRecentByUserId(String userId, int limit) {
        return loginHistoryJpaRepository.findRecentByUserId(userId, limit)
                .stream()
                .map(loginHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public List<LoginHistory> findRecentSuccessByUserId(String userId, int limit) {
        return loginHistoryJpaRepository.findRecentSuccessByUserId(userId, limit)
                .stream()
                .map(loginHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public List<LoginHistory> findRecentFailuresByUserId(String userId, int limit) {
        return loginHistoryJpaRepository.findRecentFailuresByUserId(userId, limit)
                .stream()
                .map(loginHistoryMapper::toDomain)
                .toList();
    }

    // ========================================
    // 이메일별 조회
    // ========================================

    @Override
    public Page<LoginHistory> findByEmail(String email, Pageable pageable) {
        return loginHistoryJpaRepository.findByEmailOrderByLoginAtDesc(email, pageable)
                .map(loginHistoryMapper::toDomain);
    }

    // ========================================
    // IP별 조회 (보안 감사)
    // ========================================

    @Override
    public List<LoginHistory> findByIpAddressAndPeriod(String ipAddress,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        return loginHistoryJpaRepository.findByIpAddressAndPeriod(ipAddress, from, to)
                .stream()
                .map(loginHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public long countFailuresByIpAddressSince(String ipAddress, int sinceMinutes) {
        LocalDateTime from = LocalDateTime.now().minusMinutes(sinceMinutes);
        return loginHistoryJpaRepository.countFailuresByIpAddressSince(ipAddress, from);
    }

    // ========================================
    // 기간별 조회
    // ========================================

    @Override
    public Page<LoginHistory> findByPeriod(LocalDateTime from,
                                           LocalDateTime to,
                                           Pageable pageable) {
        return loginHistoryJpaRepository.findByPeriod(from, to, pageable)
                .map(loginHistoryMapper::toDomain);
    }

    // ========================================
    // 통계
    // ========================================

    @Override
    public long countSuccessByPeriod(LocalDateTime from, LocalDateTime to) {
        return loginHistoryJpaRepository.countBySuccessAndLoginAtBetween(true, from, to);
    }

    @Override
    public long countFailuresByPeriod(LocalDateTime from, LocalDateTime to) {
        return loginHistoryJpaRepository.countBySuccessAndLoginAtBetween(false, from, to);
    }

    @Override
    public long countByFailReasonAndPeriod(String failReason,
                                           LocalDateTime from,
                                           LocalDateTime to) {
        return loginHistoryJpaRepository.countByFailReasonAndLoginAtBetween(failReason, from, to);
    }

    // ========================================
    // 배치 삭제 (법적 보관 기간 후)
    // ========================================

    @Override
    @Transactional
    public int deleteOldHistories(int retentionYears) {
        LocalDateTime threshold = LocalDateTime.now().minusYears(retentionYears);
        int count = loginHistoryJpaRepository.deleteOldHistories(threshold);
        log.info("오래된 로그인 이력 삭제 완료: count={}, threshold={}", count, threshold);
        return count;
    }
}