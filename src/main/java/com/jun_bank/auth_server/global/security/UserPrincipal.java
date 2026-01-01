package com.jun_bank.auth_server.global.security;

import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 인증된 사용자 Principal
 * <p>
 * Spring Security의 {@link UserDetails}를 구현하여
 * SecurityContext에 저장되는 인증 정보를 담습니다.
 *
 * <h3>포함 정보:</h3>
 * <ul>
 *   <li>userId: User Service의 사용자 ID (USR-xxx)</li>
 *   <li>email: 사용자 이메일</li>
 *   <li>role: 사용자 역할 (USER/ADMIN)</li>
 *   <li>userType: 사용자 유형 (CUSTOMER/SELLER)</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // SecurityContext에서 현재 사용자 조회
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
 *
 * String userId = principal.getUserId();
 * if (principal.isAdmin()) {
 *     // 관리자 로직
 * }
 * }</pre>
 *
 * @see UserDetails
 */
@Getter
public class UserPrincipal implements UserDetails {

  /**
   * User Service의 사용자 ID (USR-xxx)
   */
  private final String userId;

  /**
   * 사용자 이메일
   */
  private final String email;

  /**
   * 사용자 역할
   */
  private final UserRole role;

  /**
   * 사용자 유형 (CUSTOMER/SELLER)
   */
  private final String userType;

  /**
   * Spring Security 권한 목록
   */
  private final Collection<? extends GrantedAuthority> authorities;

  @Builder
  public UserPrincipal(String userId, String email, UserRole role, String userType) {
    this.userId = userId;
    this.email = email;
    this.role = role;
    this.userType = userType;
    // 역할 계층에 따른 권한 설정 (ADMIN은 USER 권한도 포함)
    this.authorities = role.getIncludedRoles().stream()
        .map(r -> new SimpleGrantedAuthority(r.toAuthority()))
        .collect(Collectors.toSet());
  }

  /**
   * 정적 팩토리 메서드
   *
   * @param userId   사용자 ID
   * @param email    이메일
   * @param role     역할
   * @param userType 사용자 유형
   * @return UserPrincipal 인스턴스
   */
  public static UserPrincipal of(String userId, String email, UserRole role, String userType) {
    return UserPrincipal.builder()
        .userId(userId)
        .email(email)
        .role(role)
        .userType(userType)
        .build();
  }

  // ========================================
  // UserDetails 구현
  // ========================================

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  /**
   * 비밀번호 (사용하지 않음)
   * <p>
   * JWT 기반 인증이므로 비밀번호를 저장하지 않습니다.
   * </p>
   *
   * @return null
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * 사용자명 (이메일 반환)
   *
   * @return 이메일
   */
  @Override
  public String getUsername() {
    return email;
  }

  /**
   * 계정 만료 여부
   * <p>
   * 계정 상태 관리는 AuthUser 도메인에서 처리합니다.
   * </p>
   *
   * @return true (항상 만료되지 않음)
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 계정 잠금 여부
   * <p>
   * 계정 상태 관리는 AuthUser 도메인에서 처리합니다.
   * </p>
   *
   * @return true (항상 잠금되지 않음)
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * 자격 증명 만료 여부
   *
   * @return true (항상 만료되지 않음)
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 계정 활성화 여부
   * <p>
   * 계정 상태 관리는 AuthUser 도메인에서 처리합니다.
   * </p>
   *
   * @return true (항상 활성화)
   */
  @Override
  public boolean isEnabled() {
    return true;
  }

  // ========================================
  // 편의 메서드
  // ========================================

  /**
   * 관리자 여부 확인
   *
   * @return ADMIN이면 true
   */
  public boolean isAdmin() {
    return role.isAdmin();
  }

  /**
   * 구매자 여부 확인
   *
   * @return CUSTOMER이면 true
   */
  public boolean isCustomer() {
    return "CUSTOMER".equals(userType);
  }

  /**
   * 판매자 여부 확인
   *
   * @return SELLER이면 true
   */
  public boolean isSeller() {
    return "SELLER".equals(userType);
  }

  /**
   * 특정 역할을 가지고 있는지 확인
   *
   * @param targetRole 확인할 역할
   * @return 해당 역할의 권한을 가지고 있으면 true
   */
  public boolean hasRole(UserRole targetRole) {
    return role.hasAuthority(targetRole);
  }

  @Override
  public String toString() {
    return String.format("UserPrincipal(userId=%s, email=%s, role=%s, userType=%s)",
        userId, email, role, userType);
  }
}