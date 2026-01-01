package com.jun_bank.auth_server.global.security.jwt;

import com.jun_bank.auth_server.global.config.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * JWT 키 컨트롤러
 * <p>
 * JWKS (JSON Web Key Set) 표준 엔드포인트를 제공합니다.
 * Gateway 및 다른 서비스에서 Public Key를 가져가 토큰을 검증할 수 있습니다.
 *
 * <h3>엔드포인트:</h3>
 * <ul>
 *   <li>GET /.well-known/jwks.json - JWKS 형식 (표준)</li>
 *   <li>GET /api/v1/auth/public-key - PEM 형식</li>
 * </ul>
 *
 * <h3>JWKS 응답 예시:</h3>
 * <pre>{@code
 * {
 *   "keys": [{
 *     "kty": "RSA",
 *     "alg": "RS256",
 *     "use": "sig",
 *     "kid": "jun-bank-key-v1",
 *     "n": "0vx7agoebGcQ...",
 *     "e": "AQAB"
 *   }]
 * }
 * }</pre>
 *
 * <h3>Gateway에서 사용:</h3>
 * <pre>{@code
 * // Gateway가 Auth Service에서 Public Key 가져오기
 * GET http://auth-server/.well-known/jwks.json
 *
 * // 또는 PEM 형식으로
 * GET http://auth-server/api/v1/auth/public-key
 * }</pre>
 *
 * @see RsaKeyManager
 */
@Tag(name = "JWT Key", description = "JWT Public Key 조회 API")
@RestController
@RequiredArgsConstructor
public class JwtKeyController {

  private final RsaKeyManager rsaKeyManager;
  private final JwtProperties jwtProperties;

  /**
   * JWKS (JSON Web Key Set) 형식으로 Public Key 반환
   * <p>
   * OAuth 2.0 / OpenID Connect 표준 엔드포인트입니다.
   * Gateway나 다른 서비스에서 이 엔드포인트로 Public Key를 가져가
   * JWT 토큰을 검증할 수 있습니다.
   * </p>
   *
   * @return JWKS 형식의 Public Key
   */
  @Operation(
      summary = "JWKS 조회",
      description = "JWKS (JSON Web Key Set) 표준 형식으로 Public Key를 반환합니다. " +
          "Gateway나 다른 서비스에서 JWT 검증에 사용합니다."
  )
  @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> getJwks() {
    RSAPublicKey publicKey = rsaKeyManager.getPublicKey();

    // RSA Public Key의 modulus (n)와 exponent (e) 추출
    String n = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(publicKey.getModulus().toByteArray());
    String e = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(publicKey.getPublicExponent().toByteArray());

    Map<String, Object> jwk = Map.of(
        "kty", "RSA",
        "alg", "RS256",
        "use", "sig",
        "kid", jwtProperties.getKeyId(),
        "n", n,
        "e", e
    );

    Map<String, Object> jwks = Map.of("keys", List.of(jwk));

    return ResponseEntity.ok(jwks);
  }

  /**
   * PEM 형식으로 Public Key 반환
   * <p>
   * 간단하게 PEM 형식으로 Public Key가 필요한 경우 사용합니다.
   * </p>
   *
   * @return PEM 형식의 Public Key
   */
  @Operation(
      summary = "Public Key 조회 (PEM)",
      description = "PEM 형식으로 Public Key를 반환합니다."
  )
  @GetMapping(value = "/api/v1/auth/public-key", produces = "application/x-pem-file")
  public ResponseEntity<String> getPublicKeyPem() {
    return ResponseEntity.ok(rsaKeyManager.getPublicKeyPem());
  }

  /**
   * Public Key 정보 조회 (JSON)
   * <p>
   * Public Key와 관련 메타 정보를 JSON으로 반환합니다.
   * </p>
   *
   * @return Public Key 정보
   */
  @Operation(
      summary = "Public Key 정보 조회",
      description = "Public Key와 메타 정보를 JSON 형식으로 반환합니다."
  )
  @GetMapping(value = "/api/v1/auth/public-key/info", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> getPublicKeyInfo() {
    RSAPublicKey publicKey = rsaKeyManager.getPublicKey();

    Map<String, Object> info = Map.of(
        "keyId", jwtProperties.getKeyId(),
        "algorithm", "RS256",
        "keySize", publicKey.getModulus().bitLength(),
        "publicKey", rsaKeyManager.getPublicKeyPem(),
        "issuer", jwtProperties.getIssuer()
    );

    return ResponseEntity.ok(info);
  }
}