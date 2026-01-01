package com.jun_bank.auth_server.global.security.jwt;

import com.jun_bank.auth_server.domain.auth.domain.exception.AuthException;
import com.jun_bank.auth_server.domain.auth.domain.model.UserRole;
import com.jun_bank.auth_server.global.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 프로바이더 (RS256 비대칭키)
 * <p>
 * Access Token과 Refresh Token의 생성 및 검증을 담당합니다.
 * MSA 환경을 위해 RS256 비대칭키 알고리즘을 사용합니다.
 * 키 관리는 {@link RsaKeyManager}에 위임합니다.
 *
 * <h3>비대칭키 장점:</h3>
 * <ul>
 *   <li>Private Key: Auth Server만 보유 (토큰 서명)</li>
 *   <li>Public Key: 모든 서비스에서 사용 가능 (토큰 검증)</li>
 *   <li>Public Key 탈취되어도 토큰 위조 불가</li>
 *   <li>Key Rotation 시 Auth Server만 배포</li>
 * </ul>
 *
 * <h3>Access Token 구조:</h3>
 * <pre>
 * Header: { "alg": "RS256", "typ": "JWT", "kid": "jun-bank-key-v1" }
 * Payload: {
 *   "sub": "USR-a1b2c3d4",      // userId
 *   "email": "user@example.com",
 *   "role": "USER",
 *   "userType": "CUSTOMER",
 *   "iss": "jun-bank-auth-server",
 *   "iat": 1705302600,
 *   "exp": 1705304400
 * }
 * </pre>
 *
 * <h3>Refresh Token 구조:</h3>
 * <pre>
 * Payload: {
 *   "sub": "USR-a1b2c3d4",
 *   "type": "refresh",
 *   "jti": "RTK-x1y2z3w4",      // RefreshToken ID
 *   "iss": "jun-bank-auth-server",
 *   "iat": 1705302600,
 *   "exp": 1705907400
 * }
 * </pre>
 *
 * @see JwtProperties
 * @see RsaKeyManager
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtProperties jwtProperties;
  private final RsaKeyManager rsaKeyManager;

  // ========================================
  // 토큰 생성
  // ========================================

  /**
   * Access Token 생성
   *
   * @param userId   사용자 ID (USR-xxx)
   * @param email    이메일
   * @param role     사용자 역할
   * @param userType 사용자 유형 (CUSTOMER/SELLER)
   * @return JWT Access Token
   */
  public String createAccessToken(String userId, String email, UserRole role, String userType) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpiration());
    RSAPrivateKey privateKey = rsaKeyManager.getPrivateKey();

    return Jwts.builder()
        .header()
        .keyId(jwtProperties.getKeyId())
        .type("JWT")
        .and()
        .subject(userId)
        .claim("email", email)
        .claim("role", role.name())
        .claim("userType", userType)
        .issuer(jwtProperties.getIssuer())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  /**
   * Refresh Token 생성
   *
   * @param userId         사용자 ID
   * @param refreshTokenId 리프레시 토큰 ID (RTK-xxx)
   * @return JWT Refresh Token
   */
  public String createRefreshToken(String userId, String refreshTokenId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.getRefreshToken().getExpiration());
    RSAPrivateKey privateKey = rsaKeyManager.getPrivateKey();

    return Jwts.builder()
        .header()
        .keyId(jwtProperties.getKeyId())
        .type("JWT")
        .and()
        .subject(userId)
        .claim("type", "refresh")
        .id(refreshTokenId)  // jti 클레임
        .issuer(jwtProperties.getIssuer())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  // ========================================
  // 토큰 검증
  // ========================================

  /**
   * 토큰 유효성 검증
   *
   * @param token JWT 토큰
   * @return 유효하면 true
   */
  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (AuthException e) {
      return false;
    }
  }

  /**
   * 토큰 파싱 및 클레임 추출
   *
   * @param token JWT 토큰
   * @return Claims 객체
   * @throws AuthException 토큰이 유효하지 않은 경우
   */
  public Claims parseToken(String token) {
    try {
      RSAPublicKey publicKey = rsaKeyManager.getPublicKey();
      return Jwts.parser()
          .verifyWith(publicKey)
          .requireIssuer(jwtProperties.getIssuer())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      log.debug("만료된 토큰: {}", e.getMessage());
      throw AuthException.tokenExpired();
    } catch (SignatureException e) {
      log.warn("서명 검증 실패: {}", e.getMessage());
      throw AuthException.invalidToken();
    } catch (MalformedJwtException e) {
      log.warn("잘못된 토큰 형식: {}", e.getMessage());
      throw AuthException.invalidToken();
    } catch (JwtException e) {
      log.warn("JWT 처리 오류: {}", e.getMessage());
      throw AuthException.invalidToken();
    }
  }

  // ========================================
  // 클레임 추출
  // ========================================

  /**
   * 토큰에서 사용자 ID 추출
   *
   * @param token JWT 토큰
   * @return 사용자 ID (USR-xxx)
   */
  public String getUserId(String token) {
    return parseToken(token).getSubject();
  }

  /**
   * 토큰에서 이메일 추출
   *
   * @param token JWT 토큰
   * @return 이메일
   */
  public String getEmail(String token) {
    return parseToken(token).get("email", String.class);
  }

  /**
   * 토큰에서 역할 추출
   *
   * @param token JWT 토큰
   * @return 사용자 역할
   */
  public UserRole getRole(String token) {
    String role = parseToken(token).get("role", String.class);
    return UserRole.valueOf(role);
  }

  /**
   * 토큰에서 사용자 유형 추출
   *
   * @param token JWT 토큰
   * @return 사용자 유형 (CUSTOMER/SELLER)
   */
  public String getUserType(String token) {
    return parseToken(token).get("userType", String.class);
  }

  /**
   * 토큰에서 만료 시간 추출
   *
   * @param token JWT 토큰
   * @return 만료 시간
   */
  public LocalDateTime getExpiration(String token) {
    Date expiration = parseToken(token).getExpiration();
    return expiration.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  /**
   * 토큰에서 jti (Refresh Token ID) 추출
   *
   * @param token JWT 토큰
   * @return Refresh Token ID (RTK-xxx)
   */
  public String getRefreshTokenId(String token) {
    return parseToken(token).getId();
  }

  /**
   * Refresh Token 여부 확인
   *
   * @param token JWT 토큰
   * @return Refresh Token이면 true
   */
  public boolean isRefreshToken(String token) {
    String type = parseToken(token).get("type", String.class);
    return "refresh".equals(type);
  }

  /**
   * 토큰 검증 결과를 Map으로 반환 (Gateway용)
   *
   * @param token JWT 토큰
   * @return 검증 결과 Map
   */
  public Map<String, Object> validateAndGetClaims(String token) {
    Claims claims = parseToken(token);
    return Map.of(
        "valid", true,
        "userId", claims.getSubject(),
        "email", claims.get("email", String.class),
        "role", claims.get("role", String.class),
        "userType", claims.get("userType", String.class),
        "expiresAt", claims.getExpiration().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
    );
  }

  // ========================================
  // Public Key 제공 (다른 서비스용)
  // ========================================

  /**
   * Public Key 반환 (다른 서비스에서 검증용으로 사용)
   *
   * @return RSAPublicKey 객체
   */
  public RSAPublicKey getPublicKey() {
    return rsaKeyManager.getPublicKey();
  }

  /**
   * Public Key를 PEM 형식으로 반환
   * <p>
   * API 엔드포인트를 통해 다른 서비스에서 Public Key를 가져갈 수 있습니다.
   * </p>
   *
   * @return PEM 형식의 Public Key 문자열
   */
  public String getPublicKeyPem() {
    return rsaKeyManager.getPublicKeyPem();
  }
}