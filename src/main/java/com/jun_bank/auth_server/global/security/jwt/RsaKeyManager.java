package com.jun_bank.auth_server.global.security.jwt;

import com.jun_bank.auth_server.global.config.JwtProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 키 매니저
 * <p>
 * RSA 키 쌍을 생성, 로딩, 저장하는 역할을 담당합니다.
 * 개발 환경에서는 키가 없으면 자동 생성하고,
 * 프로덕션에서는 환경변수로 주입된 키를 사용합니다.
 *
 * <h3>키 로딩 우선순위:</h3>
 * <ol>
 *   <li>환경변수로 키 값이 제공된 경우 (프로덕션)</li>
 *   <li>지정된 파일 경로에 키 파일이 있는 경우</li>
 *   <li>기본 디렉토리(data/keys/)에 키 파일이 있는 경우</li>
 *   <li>키가 없으면 자동 생성 후 저장 (개발용)</li>
 * </ol>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class JwtTokenProvider {
 *     private final RsaKeyManager rsaKeyManager;
 *
 *     public String createToken() {
 *         return Jwts.builder()
 *             .signWith(rsaKeyManager.getPrivateKey(), Jwts.SIG.RS256)
 *             .compact();
 *     }
 * }
 * }</pre>
 *
 * @see JwtProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RsaKeyManager {

  private static final String PRIVATE_KEY_FILE = "private.pem";
  private static final String PUBLIC_KEY_FILE = "public.pem";
  private static final int KEY_SIZE = 2048;

  private final JwtProperties jwtProperties;

  @Getter
  private RSAPrivateKey privateKey;

  @Getter
  private RSAPublicKey publicKey;

  /**
   * 초기화: 키 로딩 또는 생성
   */
  @PostConstruct
  public void init() {
    try {
      loadOrGenerateKeys();
      log.info("RSA 키 초기화 완료 - keyId: {}", jwtProperties.getKeyId());
    } catch (Exception e) {
      throw new IllegalStateException("RSA 키 초기화 실패", e);
    }
  }

  /**
   * 키를 로딩하거나 없으면 생성한다.
   */
  private void loadOrGenerateKeys() throws Exception {
    // 1순위: 환경변수에서 키 값 로딩 (프로덕션)
    if (jwtProperties.hasPrivateKeyValue() && jwtProperties.hasPublicKeyValue()) {
      log.info("환경변수에서 RSA 키 로딩");
      loadKeysFromEnv();
      return;
    }

    // 2순위: 지정된 파일 경로에서 로딩
    if (jwtProperties.hasPrivateKeyPath() && jwtProperties.hasPublicKeyPath()) {
      Path privatePath = Paths.get(jwtProperties.getPrivateKeyPath());
      Path publicPath = Paths.get(jwtProperties.getPublicKeyPath());

      if (Files.exists(privatePath) && Files.exists(publicPath)) {
        log.info("지정된 경로에서 RSA 키 로딩: {}", privatePath.getParent());
        loadKeysFromFiles(privatePath, publicPath);
        return;
      }
    }

    // 3순위: 기본 디렉토리에서 로딩
    Path keyDir = Paths.get(jwtProperties.getKeyDirectory());
    Path privatePath = keyDir.resolve(PRIVATE_KEY_FILE);
    Path publicPath = keyDir.resolve(PUBLIC_KEY_FILE);

    if (Files.exists(privatePath) && Files.exists(publicPath)) {
      log.info("기본 디렉토리에서 RSA 키 로딩: {}", keyDir.toAbsolutePath());
      loadKeysFromFiles(privatePath, publicPath);
      return;
    }

    // 4순위: 키 자동 생성 (개발 환경)
    log.info("RSA 키 파일이 없어 새로 생성합니다: {}", keyDir.toAbsolutePath());
    generateAndSaveKeys(keyDir);
  }

  /**
   * 환경변수에서 키 로딩 (Base64 인코딩)
   */
  private void loadKeysFromEnv() throws Exception {
    byte[] privateKeyBytes = Base64.getDecoder().decode(jwtProperties.getPrivateKey());
    byte[] publicKeyBytes = Base64.getDecoder().decode(jwtProperties.getPublicKey());

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
    this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
    this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
  }

  /**
   * 파일에서 키 로딩 (PEM 형식)
   */
  private void loadKeysFromFiles(Path privatePath, Path publicPath) throws Exception {
    String privateKeyPem = Files.readString(privatePath);
    String publicKeyPem = Files.readString(publicPath);

    this.privateKey = parsePrivateKey(privateKeyPem);
    this.publicKey = parsePublicKey(publicKeyPem);
  }

  /**
   * 새 키 쌍을 생성하고 파일로 저장
   */
  private void generateAndSaveKeys(Path keyDir) throws Exception {
    // 키 쌍 생성
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(KEY_SIZE);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
    this.publicKey = (RSAPublicKey) keyPair.getPublic();

    // 디렉토리 생성
    Files.createDirectories(keyDir);

    // PEM 형식으로 저장
    Path privatePath = keyDir.resolve(PRIVATE_KEY_FILE);
    Path publicPath = keyDir.resolve(PUBLIC_KEY_FILE);

    Files.writeString(privatePath, toPemFormat(privateKey.getEncoded(), "PRIVATE KEY"));
    Files.writeString(publicPath, toPemFormat(publicKey.getEncoded(), "PUBLIC KEY"));

    log.info("RSA 키 파일 생성 완료:");
    log.info("  Private Key: {}", privatePath.toAbsolutePath());
    log.info("  Public Key: {}", publicPath.toAbsolutePath());
    log.warn("⚠️  주의: 생성된 private.pem 파일은 절대 Git에 커밋하지 마세요!");
  }

  /**
   * PEM 문자열을 RSAPrivateKey로 파싱
   */
  private RSAPrivateKey parsePrivateKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String base64 = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(base64);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
  }

  /**
   * PEM 문자열을 RSAPublicKey로 파싱
   */
  private RSAPublicKey parsePublicKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String base64 = pem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(base64);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
  }

  /**
   * 바이트 배열을 PEM 형식 문자열로 변환
   */
  private String toPemFormat(byte[] keyBytes, String type) {
    String base64 = Base64.getEncoder().encodeToString(keyBytes);
    StringBuilder pem = new StringBuilder();

    pem.append("-----BEGIN ").append(type).append("-----\n");

    // 64자씩 줄바꿈
    for (int i = 0; i < base64.length(); i += 64) {
      pem.append(base64, i, Math.min(i + 64, base64.length()));
      pem.append("\n");
    }

    pem.append("-----END ").append(type).append("-----\n");

    return pem.toString();
  }

  // ========================================
  // Public Key 제공 메서드
  // ========================================

  /**
   * Public Key를 PEM 형식으로 반환
   *
   * @return PEM 형식의 Public Key 문자열
   */
  public String getPublicKeyPem() {
    return toPemFormat(publicKey.getEncoded(), "PUBLIC KEY");
  }

  /**
   * Public Key를 Base64로 인코딩하여 반환
   *
   * @return Base64 인코딩된 Public Key
   */
  public String getPublicKeyBase64() {
    return Base64.getEncoder().encodeToString(publicKey.getEncoded());
  }
}