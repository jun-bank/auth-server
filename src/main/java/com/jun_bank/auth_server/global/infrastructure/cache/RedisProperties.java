package com.jun_bank.auth_server.global.infrastructure.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 설정 프로퍼티
 * <p>
 * Primary-Replica 구조의 Redis 설정을 관리합니다.
 *
 * <h3>설정 예:</h3>
 * <pre>
 * redis:
 *   primary:
 *     host: localhost
 *     port: 6379
 *   replica:
 *     host: localhost
 *     port: 6380
 *   password: ${REDIS_PASSWORD:}
 *   timeout: 3000
 *   pool:
 *     max-active: 10
 *     max-idle: 5
 *     min-idle: 1
 *     max-wait: 3000
 * </pre>
 *
 * <h3>환경변수 매핑:</h3>
 * <ul>
 *   <li>REDIS_PRIMARY_HOST → redis.primary.host</li>
 *   <li>REDIS_PRIMARY_PORT → redis.primary.port</li>
 *   <li>REDIS_REPLICA_HOST → redis.replica.host</li>
 *   <li>REDIS_REPLICA_PORT → redis.replica.port</li>
 *   <li>REDIS_PASSWORD → redis.password</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

  /**
   * Primary 노드 설정 (Write)
   */
  private Node primary = new Node();

  /**
   * Replica 노드 설정 (Read)
   */
  private Node replica = new Node();

  /**
   * Redis 비밀번호 (선택)
   */
  private String password;

  /**
   * 연결 타임아웃 (밀리초)
   */
  private long timeout = 3000;

  /**
   * 커넥션 풀 설정
   */
  private Pool pool = new Pool();

  /**
   * Redis 노드 설정
   */
  @Getter
  @Setter
  public static class Node {
    /**
     * 호스트
     */
    private String host = "localhost";

    /**
     * 포트
     */
    private int port = 6379;

    /**
     * 데이터베이스 인덱스 (기본: 0)
     */
    private int database = 0;
  }

  /**
   * 커넥션 풀 설정
   */
  @Getter
  @Setter
  public static class Pool {
    /**
     * 최대 활성 연결 수
     */
    private int maxActive = 10;

    /**
     * 최대 유휴 연결 수
     */
    private int maxIdle = 5;

    /**
     * 최소 유휴 연결 수
     */
    private int minIdle = 1;

    /**
     * 연결 대기 최대 시간 (밀리초)
     */
    private long maxWait = 3000;
  }

  // ========================================
  // Helper Methods
  // ========================================

  /**
   * Primary 노드 주소 반환
   *
   * @return "host:port" 형식
   */
  public String getPrimaryAddress() {
    return primary.getHost() + ":" + primary.getPort();
  }

  /**
   * Replica 노드 주소 반환
   *
   * @return "host:port" 형식
   */
  public String getReplicaAddress() {
    return replica.getHost() + ":" + replica.getPort();
  }

  /**
   * 비밀번호 존재 여부
   *
   * @return 비밀번호가 설정되어 있으면 true
   */
  public boolean hasPassword() {
    return password != null && !password.isBlank();
  }

  /**
   * Replica가 설정되어 있는지 확인
   * <p>
   * Primary와 다른 호스트/포트면 Replica가 있는 것으로 판단
   * </p>
   *
   * @return Replica가 설정되어 있으면 true
   */
  public boolean hasReplica() {
    return !primary.getHost().equals(replica.getHost())
        || primary.getPort() != replica.getPort();
  }
}