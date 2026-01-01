package com.jun_bank.auth_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 * <p>
 * application.yml의 jwt.* 설정을 바인딩합니다.
 * RS256 비대칭키 기반 JWT를 지원합니다.
 *
 * <h3>설정 예시 (auth-server.yml):</h3>
 * <pre>
 * jwt:
 *   issuer: jun-bank-auth-server
 *   key-id: jun-bank-key-v1
 *   key-directory: data/keys
 *   # 프로덕션: 환경변수로 키 값 주입 (Base64 인코딩)
 *   private-key: ${JWT_PRIVATE_KEY:}
 *   public-key: ${JWT_PUBLIC_KEY:}
 *   # 또는 파일 경로로 지정
 *   private-key-path: ${JWT_PRIVATE_KEY_PATH:}
 *   public-key-path: ${JWT_PUBLIC_KEY_PATH:}
 *   access-token:
 *     expiration: 1800000  # 30분 (밀리초)
 *   refresh-token:
 *     expiration: 604800000  # 7일 (밀리초)
 * </pre>
 *
 * <h3>키 로딩 우선순위:</h3>
 * <ol>
 *   <li>환경변수로 키 값이 제공된 경우 (프로덕션) - private-key, public-key</li>
 *   <li>지정된 파일 경로에 키 파일이 있는 경우 - private-key-path, public-key-path</li>
 *   <li>기본 디렉토리(key-directory)에 키 파일이 있는 경우</li>
 *   <li>키가 없으면 자동 생성 후 저장 (개발용)</li>
 * </ol>
 *
 * <h3>RS256 비대칭키 장점 (MSA 환경):</h3>
 * <ul>
 *   <li>Private Key: Auth Server만 보유 → 토큰 서명</li>
 *   <li>Public Key: 모든 서비스에서 사용 → 토큰 검증</li>
 *   <li>Public Key 탈취되어도 토큰 위조 불가</li>
 *   <li>Key Rotation 시 Auth Server만 배포</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  /**
   * JWT 발급자 (iss 클레임)
   */
  private String issuer = "jun-bank-auth-server";

  /**
   * 키 ID (kid 클레임)
   * <p>
   * Key Rotation 시 어떤 키로 서명했는지 식별합니다.
   * JWT 헤더에 kid 클레임으로 포함됩니다.
   * </p>
   */
  private String keyId = "jun-bank-key-v1";

  /**
   * 키 저장 디렉토리 경로
   * <p>
   * 키 파일이 없을 때 자동 생성되는 위치입니다.
   * 기본값: data/keys
   * </p>
   */
  private String keyDirectory = "data/keys";

  /**
   * Private Key 파일 경로 (선택)
   * <p>
   * 환경변수: JWT_PRIVATE_KEY_PATH
   * </p>
   */
  private String privateKeyPath;

  /**
   * Public Key 파일 경로 (선택)
   * <p>
   * 환경변수: JWT_PUBLIC_KEY_PATH
   * </p>
   */
  private String publicKeyPath;

  /**
   * Private Key 값 (Base64 인코딩, 환경변수 주입용)
   * <p>
   * 프로덕션 환경에서 환경변수로 직접 키 값을 주입할 때 사용합니다.
   * 환경변수: JWT_PRIVATE_KEY
   * </p>
   */
  private String privateKey;

  /**
   * Public Key 값 (Base64 인코딩, 환경변수 주입용)
   * <p>
   * 프로덕션 환경에서 환경변수로 직접 키 값을 주입할 때 사용합니다.
   * 환경변수: JWT_PUBLIC_KEY
   * </p>
   */
  private String publicKey;

  /**
   * Access Token 설정
   */
  private AccessToken accessToken = new AccessToken();

  /**
   * Refresh Token 설정
   */
  private RefreshToken refreshToken = new RefreshToken();

  /**
   * Access Token 설정 클래스
   */
  @Getter
  @Setter
  public static class AccessToken {

    /**
     * 만료 시간 (밀리초)
     * <p>기본값: 30분 (1800000ms)</p>
     */
    private long expiration = 1800000L;

    /**
     * 헤더 이름
     * <p>기본값: Authorization</p>
     */
    private String header = "Authorization";

    /**
     * 토큰 접두사
     * <p>기본값: Bearer</p>
     */
    private String prefix = "Bearer";
  }

  /**
   * Refresh Token 설정 클래스
   */
  @Getter
  @Setter
  public static class RefreshToken {

    /**
     * 만료 시간 (밀리초)
     * <p>기본값: 7일 (604800000ms)</p>
     */
    private long expiration = 604800000L;
  }

  // ========================================
  // 헬퍼 메서드
  // ========================================

  /**
   * 환경변수로 Private Key가 제공되었는지 확인
   *
   * @return Private Key가 환경변수로 제공되면 true
   */
  public boolean hasPrivateKeyValue() {
    return privateKey != null && !privateKey.isBlank();
  }

  /**
   * 환경변수로 Public Key가 제공되었는지 확인
   *
   * @return Public Key가 환경변수로 제공되면 true
   */
  public boolean hasPublicKeyValue() {
    return publicKey != null && !publicKey.isBlank();
  }

  /**
   * Private Key 파일 경로가 지정되었는지 확인
   *
   * @return 경로가 유효하면 true
   */
  public boolean hasPrivateKeyPath() {
    return privateKeyPath != null && !privateKeyPath.isBlank();
  }

  /**
   * Public Key 파일 경로가 지정되었는지 확인
   *
   * @return 경로가 유효하면 true
   */
  public boolean hasPublicKeyPath() {
    return publicKeyPath != null && !publicKeyPath.isBlank();
  }
}