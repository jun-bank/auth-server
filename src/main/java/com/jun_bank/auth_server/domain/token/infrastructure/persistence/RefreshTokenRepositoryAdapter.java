package com.jun_bank.auth_server.domain.token.infrastructure.persistence;

import com.jun_bank.auth_server.domain.token.application.port.out.RefreshTokenRepository;
import com.jun_bank.auth_server.domain.token.domain.model.RefreshToken;
import com.jun_bank.auth_server.domain.token.infrastructure.cache.RefreshTokenCacheRepository;
import com.jun_bank.auth_server.domain.token.infrastructure.cache.RefreshTokenCacheRepository.SaveResult;
import com.jun_bank.auth_server.domain.token.infrastructure.persistence.entity.RefreshTokenEntity;
import com.jun_bank.auth_server.domain.token.infrastructure.persistence.jpa.RefreshTokenJpaRepository;
import com.jun_bank.auth_server.domain.token.infrastructure.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RefreshToken Repository Adapter (Output Adapter)
 * <p>
 * Application Layer의 {@link RefreshTokenRepository} Port를 구현합니다.
 * JPA Repository와 Cache Repository를 조합하여 영속성 로직을 처리합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>Domain ↔ Entity 변환 (Mapper 사용)</li>
 *   <li>JPA Repository 호출 (영구 저장)</li>
 *   <li>Cache Repository 호출 (빠른 조회, 원자적 처리)</li>
 *   <li>트랜잭션 관리</li>
 * </ul>
 *
 * <h3>캐시 전략:</h3>
 * <ul>
 *   <li>저장: DB 저장 → Cache 동기화 (Lua Script로 원자적 처리)</li>
 *   <li>조회: Cache 우선 → Cache Miss 시 DB 조회</li>
 *   <li>폐기: Cache + DB 동시 처리</li>
 * </ul>
 *
 * <h3>동시성 처리:</h3>
 * <ul>
 *   <li>같은 디바이스 토큰 교체 → Lua Script (원자적)</li>
 *   <li>최대 세션 초과 처리 → Lua Script (원자적)</li>
 *   <li>전체 로그아웃 → Lua Script (원자적)</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenCacheRepository refreshTokenCacheRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    // ========================================
    // 저장
    // ========================================

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        // 1. DB 저장
        RefreshTokenEntity entity = refreshTokenMapper.toEntity(refreshToken);
        entity = refreshTokenJpaRepository.save(entity);

        RefreshToken saved = refreshTokenMapper.toDomain(entity);

        // 2. Cache 저장 (Lua Script - 원자적)
        // 디바이스 ID로 User-Agent 해시 사용
        String deviceId = generateDeviceId(refreshToken.getDeviceInfo());
        SaveResult cacheResult = refreshTokenCacheRepository.save(saved, deviceId);

        if (cacheResult.isSuccess()) {
            log.debug("RefreshToken 저장 완료: userId={}, status={}",
                    saved.getUserId(), cacheResult.status().name());

            // 기존 토큰이 교체/삭제된 경우 DB에서도 폐기
            if (cacheResult.removedToken() != null) {
                refreshTokenJpaRepository.revokeByToken(cacheResult.removedToken());
                log.debug("기존 토큰 폐기: removedToken={}...",
                        cacheResult.removedToken().substring(0, Math.min(10, cacheResult.removedToken().length())));
            }
        } else {
            log.warn("RefreshToken 캐시 저장 실패: userId={}, error={}",
                    saved.getUserId(), cacheResult.errorMessage());
        }

        return saved;
    }

    /**
     * 최대 세션 수를 지정하여 저장
     *
     * @param refreshToken 저장할 토큰
     * @param maxSessions  최대 허용 세션 수
     * @return 저장된 토큰
     */
    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken, int maxSessions) {
        // 1. DB 저장
        RefreshTokenEntity entity = refreshTokenMapper.toEntity(refreshToken);
        entity = refreshTokenJpaRepository.save(entity);

        RefreshToken saved = refreshTokenMapper.toDomain(entity);

        // 2. Cache 저장 (최대 세션 수 지정)
        String deviceId = generateDeviceId(refreshToken.getDeviceInfo());
        SaveResult cacheResult = refreshTokenCacheRepository.save(saved, deviceId, maxSessions);

        if (cacheResult.isSuccess() && cacheResult.removedToken() != null) {
            refreshTokenJpaRepository.revokeByToken(cacheResult.removedToken());
        }

        return saved;
    }

    // ========================================
    // 단건 조회
    // ========================================

    @Override
    public Optional<RefreshToken> findById(String refreshTokenId) {
        return refreshTokenJpaRepository.findById(refreshTokenId)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        // 1. Cache 우선 조회
        Optional<RefreshToken> cached = refreshTokenCacheRepository.findByToken(token);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Cache Miss → DB 조회
        log.debug("RefreshToken 캐시 미스: token={}...", token.substring(0, Math.min(10, token.length())));
        return refreshTokenJpaRepository.findByToken(token)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findValidToken(String token) {
        // 1. Cache 우선 조회 (유효성 검증 포함)
        Optional<RefreshToken> cached = refreshTokenCacheRepository.findValidToken(token);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Cache Miss → DB 조회
        return refreshTokenJpaRepository.findValidToken(token, LocalDateTime.now())
                .map(refreshTokenMapper::toDomain);
    }

    // ========================================
    // 사용자별 조회
    // ========================================

    @Override
    public List<RefreshToken> findByUserId(String userId) {
        // Cache에서 조회 (빠름)
        List<RefreshToken> cached = refreshTokenCacheRepository.findByUserId(userId);
        if (!cached.isEmpty()) {
            return cached;
        }

        // Cache 비어있으면 DB 조회
        return refreshTokenJpaRepository.findByUserId(userId)
                .stream()
                .map(refreshTokenMapper::toDomain)
                .toList();
    }

    @Override
    public List<RefreshToken> findValidTokensByUserId(String userId) {
        return refreshTokenJpaRepository.findValidTokensByUserId(userId, LocalDateTime.now())
                .stream()
                .map(refreshTokenMapper::toDomain)
                .toList();
    }

    @Override
    public long countValidTokensByUserId(String userId) {
        // Cache에서 빠르게 카운트
        long cacheCount = refreshTokenCacheRepository.countByUserId(userId);
        if (cacheCount > 0) {
            return cacheCount;
        }

        // Cache에 없으면 DB 조회
        return refreshTokenJpaRepository.countValidTokensByUserId(userId, LocalDateTime.now());
    }

    // ========================================
    // 토큰 폐기
    // ========================================

    @Override
    @Transactional
    public void revokeByToken(String token) {
        // 토큰으로 userId 조회 (Cache에서 폐기하려면 userId 필요)
        refreshTokenJpaRepository.findByToken(token)
                .ifPresent(entity -> {
                    // 1. DB 폐기
                    entity.revoke();

                    // 2. Cache 폐기
                    refreshTokenCacheRepository.revoke(token, entity.getUserId());

                    log.debug("RefreshToken 폐기 완료: token={}...",
                            token.substring(0, Math.min(10, token.length())));
                });
    }

    @Override
    @Transactional
    public int revokeAllByUserId(String userId) {
        // 1. DB 전체 폐기
        int count = refreshTokenJpaRepository.revokeAllByUserId(userId);

        // 2. Cache 전체 폐기 (Lua Script - 원자적)
        refreshTokenCacheRepository.revokeAll(userId);

        log.info("RefreshToken 전체 폐기 완료: userId={}, count={}", userId, count);
        return count;
    }

    // ========================================
    // 정리 (배치용)
    // ========================================

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        int count = refreshTokenJpaRepository.deleteExpiredTokens(LocalDateTime.now());
        if (count > 0) {
            log.info("만료된 토큰 삭제 완료: count={}", count);
        }
        return count;
    }

    @Override
    @Transactional
    public int deleteOldRevokedTokens(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int count = refreshTokenJpaRepository.deleteOldRevokedTokens(threshold);
        if (count > 0) {
            log.info("폐기된 오래된 토큰 삭제 완료: count={}, retentionDays={}", count, retentionDays);
        }
        return count;
    }

    // ========================================
    // 존재 여부 확인
    // ========================================

    @Override
    public boolean existsByToken(String token) {
        return refreshTokenJpaRepository.existsByToken(token);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * 디바이스 ID 생성 (User-Agent 해시)
     * <p>
     * 같은 디바이스에서 로그인하면 기존 토큰을 교체합니다.
     * </p>
     */
    private String generateDeviceId(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.isBlank()) {
            return "unknown";
        }
        return String.valueOf(deviceInfo.hashCode());
    }
}