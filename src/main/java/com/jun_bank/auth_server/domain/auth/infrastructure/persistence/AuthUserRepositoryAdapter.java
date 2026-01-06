package com.jun_bank.auth_server.domain.auth.infrastructure.persistence;

import com.jun_bank.auth_server.domain.auth.application.port.out.AuthUserRepository;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUser;
import com.jun_bank.auth_server.domain.auth.domain.model.AuthUserStatus;
import com.jun_bank.auth_server.domain.auth.infrastructure.cache.LoginAttemptCacheRepository;
import com.jun_bank.auth_server.domain.auth.infrastructure.cache.LoginAttemptCacheRepository.AttemptResult;
import com.jun_bank.auth_server.domain.auth.infrastructure.cache.LoginAttemptCacheRepository.AttemptStatus;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.entity.AuthUserEntity;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.jpa.AuthUserJpaRepository;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AuthUser Repository Adapter (Output Adapter)
 * <p>
 * Application Layer의 {@link AuthUserRepository} Port를 구현합니다.
 * JPA Repository와 Cache Repository를 조합하여 영속성 로직을 처리합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>Domain ↔ Entity 변환 (Mapper 사용)</li>
 *   <li>JPA Repository 호출</li>
 *   <li>로그인 시도 캐시 관리 (LoginAttemptCacheRepository)</li>
 *   <li>트랜잭션 관리</li>
 * </ul>
 *
 * <h3>로그인 시도 처리:</h3>
 * <ul>
 *   <li>로그인 실패 → Redis에 카운트 증가</li>
 *   <li>로그인 성공 → Redis 카운트 초기화</li>
 *   <li>잠금 확인 → Redis에서 확인 (빠름)</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthUserRepositoryAdapter implements AuthUserRepository {

  private final AuthUserJpaRepository authUserJpaRepository;
  private final AuthUserMapper authUserMapper;
  private final LoginAttemptCacheRepository loginAttemptCacheRepository;

  // ========================================
  // 저장
  // ========================================

  @Override
  @Transactional
  public AuthUser save(AuthUser authUser) {
    if (authUser.isNew()) {
      // 신규 저장
      AuthUserEntity entity = authUserMapper.toEntity(authUser);
      entity = authUserJpaRepository.save(entity);
      log.debug("AuthUser 신규 저장: email={}", authUser.getEmail().value());
      return authUserMapper.toDomain(entity);
    } else {
      // 기존 업데이트 (더티 체킹)
      AuthUserEntity entity = authUserJpaRepository
              .findById(authUser.getAuthUserId().value())
              .orElseThrow(() -> new IllegalStateException(
                      "AuthUser not found: " + authUser.getAuthUserId().value()));

      authUserMapper.updateEntity(entity, authUser);
      log.debug("AuthUser 수정 완료: email={}", authUser.getEmail().value());
      return authUserMapper.toDomain(entity);
    }
  }

  // ========================================
  // 단건 조회
  // ========================================

  @Override
  public Optional<AuthUser> findById(String authUserId) {
    return authUserJpaRepository.findByAuthUserIdAndIsDeletedFalse(authUserId)
            .map(authUserMapper::toDomain);
  }

  @Override
  public Optional<AuthUser> findByEmail(String email) {
    return authUserJpaRepository.findByEmailAndIsDeletedFalse(email)
            .map(authUserMapper::toDomain);
  }

  @Override
  public Optional<AuthUser> findByUserId(String userId) {
    return authUserJpaRepository.findByUserIdAndIsDeletedFalse(userId)
            .map(authUserMapper::toDomain);
  }

  // ========================================
  // 존재 여부 확인
  // ========================================

  @Override
  public boolean existsByEmail(String email) {
    return authUserJpaRepository.existsByEmailAndIsDeletedFalse(email);
  }

  @Override
  public boolean existsByUserId(String userId) {
    return authUserJpaRepository.existsByUserIdAndIsDeletedFalse(userId);
  }

  // ========================================
  // 상태별 조회
  // ========================================

  @Override
  public List<AuthUser> findByStatus(AuthUserStatus status) {
    return authUserJpaRepository.findByStatusAndIsDeletedFalse(status)
            .stream()
            .map(authUserMapper::toDomain)
            .toList();
  }

  @Override
  public List<AuthUser> findExpiredLockedUsers() {
    return authUserJpaRepository.findExpiredLockedUsers(AuthUserStatus.LOCKED, LocalDateTime.now())
            .stream()
            .map(authUserMapper::toDomain)
            .toList();
  }

  // ========================================
  // 배치 조회
  // ========================================

  @Override
  public List<AuthUser> findByUserIds(List<String> userIds) {
    return authUserJpaRepository.findByUserIdInAndIsDeletedFalse(userIds)
            .stream()
            .map(authUserMapper::toDomain)
            .toList();
  }

  // ========================================
  // 배치 업데이트
  // ========================================

  @Override
  @Transactional
  public int unlockExpiredAccounts() {
    int count = authUserJpaRepository.unlockExpiredAccounts(
            AuthUserStatus.LOCKED,
            AuthUserStatus.ACTIVE,
            LocalDateTime.now()
    );
    if (count > 0) {
      log.info("만료된 잠금 해제 완료: count={}", count);
    }
    return count;
  }

  // ========================================
  // 삭제 (Soft Delete)
  // ========================================

  @Override
  @Transactional
  public void deleteByUserId(String userId, String deletedBy) {
    authUserJpaRepository.findByUserIdAndIsDeletedFalse(userId)
            .ifPresent(entity -> {
              entity.delete(deletedBy);
              log.info("AuthUser Soft Delete 완료: userId={}, deletedBy={}", userId, deletedBy);
            });
  }

  // ========================================
  // 로그인 시도 관리 (Cache)
  // ========================================

  /**
   * 로그인 실패 기록
   * <p>
   * Redis에 원자적으로 실패 횟수를 증가시킵니다.
   * 최대 횟수 초과 시 자동으로 계정이 잠깁니다.
   * </p>
   *
   * @param email 이메일
   * @return LoginAttemptResult (상태, 시도 횟수, 남은 잠금 시간)
   */
  @Override
  public LoginAttemptResult recordLoginFailure(String email) {
    AttemptResult result = loginAttemptCacheRepository.recordFailure(email);

    // 잠금 시 DB 상태도 업데이트
    if (result.justLocked()) {
      updateAuthUserStatusToLocked(email);
    }

    return toLoginAttemptResult(result);
  }

  /**
   * 로그인 성공 기록
   * <p>
   * Redis의 실패 카운터를 초기화합니다.
   * </p>
   *
   * @param email 이메일
   */
  @Override
  public void recordLoginSuccess(String email) {
    loginAttemptCacheRepository.recordSuccess(email);
  }

  /**
   * 계정 잠금 여부 확인
   * <p>
   * Redis에서 빠르게 확인합니다.
   * </p>
   *
   * @param email 이메일
   * @return 잠금 상태면 true
   */
  @Override
  public boolean isAccountLocked(String email) {
    return loginAttemptCacheRepository.isLocked(email);
  }

  /**
   * 현재 실패 횟수 조회
   *
   * @param email 이메일
   * @return 실패 횟수
   */
  @Override
  public int getFailedAttempts(String email) {
    return loginAttemptCacheRepository.getFailedAttempts(email);
  }

  /**
   * 남은 잠금 시간 조회 (초)
   *
   * @param email 이메일
   * @return 남은 시간 (초), 잠금 상태가 아니면 0
   */
  @Override
  public long getRemainingLockSeconds(String email) {
    return loginAttemptCacheRepository.getRemainingLockSeconds(email);
  }

  // ========================================
  // Private Helper Methods
  // ========================================

  /**
   * AuthUser 상태를 LOCKED로 업데이트
   */
  @Transactional
  protected void updateAuthUserStatusToLocked(String email) {
    authUserJpaRepository.findByEmailAndIsDeletedFalse(email)
            .ifPresent(entity -> {
              entity.updateStatus(AuthUserStatus.LOCKED);
              log.warn("계정 잠금 처리: email={}", email);
            });
  }

  /**
   * Cache의 AttemptResult → Port의 LoginAttemptResult 변환
   */
  private LoginAttemptResult toLoginAttemptResult(AttemptResult result) {
    LoginAttemptStatus status = switch (result.status()) {
      case OK -> LoginAttemptStatus.OK;
      case LOCKED -> LoginAttemptStatus.LOCKED;
      case ALREADY_LOCKED -> LoginAttemptStatus.ALREADY_LOCKED;
      default -> LoginAttemptStatus.ERROR;
    };

    return new LoginAttemptResult(
            result.email(),
            status,
            result.attempts(),
            result.remainingSeconds(),
            result.errorMessage()
    );
  }
}