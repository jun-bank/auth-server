package com.jun_bank.auth_server.global.security;

import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SecurityContext에서 현재 사용자 정보 조회 유틸리티
 * <p>
 * SecurityContext에 저장된 인증 정보를 쉽게 조회할 수 있는 유틸리티입니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class SomeService {
 *     private final SecurityContextUtil securityContextUtil;
 *
 *     public void doSomething() {
 *         String userId = securityContextUtil.getCurrentUserId();
 *         if (securityContextUtil.isAdmin()) {
 *             // 관리자 로직
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see UserPrincipal
 */
@Component
public class SecurityContextUtil {

  /**
   * 현재 인증된 사용자 ID 조회
   *
   * @return 사용자 ID (인증 정보 없으면 null)
   */
  public String getCurrentUserId() {
    return getCurrentPrincipal()
        .map(UserPrincipal::getUserId)
        .orElse(null);
  }

  /**
   * 현재 인증된 사용자 이메일 조회
   *
   * @return 이메일 (인증 정보 없으면 null)
   */
  public String getCurrentEmail() {
    return getCurrentPrincipal()
        .map(UserPrincipal::getEmail)
        .orElse(null);
  }

  /**
   * 현재 인증된 사용자 역할 조회
   *
   * @return 사용자 역할 (인증 정보 없으면 null)
   */
  public UserRole getCurrentRole() {
    return getCurrentPrincipal()
        .map(UserPrincipal::getRole)
        .orElse(null);
  }

  /**
   * 현재 인증된 사용자 유형 조회
   *
   * @return 사용자 유형 (인증 정보 없으면 null)
   */
  public String getCurrentUserType() {
    return getCurrentPrincipal()
        .map(UserPrincipal::getUserType)
        .orElse(null);
  }

  /**
   * 현재 사용자가 인증되어 있는지 확인
   *
   * @return 인증 여부
   */
  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserPrincipal;
  }

  /**
   * 현재 사용자가 관리자인지 확인
   *
   * @return 관리자 여부
   */
  public boolean isAdmin() {
    return getCurrentPrincipal()
        .map(UserPrincipal::isAdmin)
        .orElse(false);
  }

  /**
   * 현재 사용자가 구매자인지 확인
   *
   * @return 구매자 여부
   */
  public boolean isCustomer() {
    return getCurrentPrincipal()
        .map(UserPrincipal::isCustomer)
        .orElse(false);
  }

  /**
   * 현재 사용자가 판매자인지 확인
   *
   * @return 판매자 여부
   */
  public boolean isSeller() {
    return getCurrentPrincipal()
        .map(UserPrincipal::isSeller)
        .orElse(false);
  }

  /**
   * 현재 사용자가 특정 역할을 가지고 있는지 확인
   *
   * @param role 확인할 역할
   * @return 역할 보유 여부
   */
  public boolean hasRole(UserRole role) {
    return getCurrentPrincipal()
        .map(p -> p.hasRole(role))
        .orElse(false);
  }

  /**
   * 현재 UserPrincipal 조회
   *
   * @return Optional로 감싼 UserPrincipal
   */
  public Optional<UserPrincipal> getCurrentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserPrincipal userPrincipal) {
      return Optional.of(userPrincipal);
    }

    return Optional.empty();
  }

  /**
   * 현재 사용자가 특정 사용자 ID와 일치하는지 확인
   * <p>
   * 리소스 소유자 검증에 사용됩니다.
   * </p>
   *
   * @param targetUserId 비교할 사용자 ID
   * @return 일치 여부
   */
  public boolean isCurrentUser(String targetUserId) {
    String currentUserId = getCurrentUserId();
    return currentUserId != null && currentUserId.equals(targetUserId);
  }

  /**
   * 현재 사용자가 특정 사용자이거나 관리자인지 확인
   * <p>
   * "본인 또는 관리자만 접근 가능" 검증에 사용됩니다.
   * </p>
   *
   * @param targetUserId 비교할 사용자 ID
   * @return 본인 또는 관리자 여부
   */
  public boolean isCurrentUserOrAdmin(String targetUserId) {
    return isCurrentUser(targetUserId) || isAdmin();
  }
}