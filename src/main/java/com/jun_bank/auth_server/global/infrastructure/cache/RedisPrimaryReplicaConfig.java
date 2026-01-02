package com.jun_bank.auth_server.global.infrastructure.cache;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;

/**
 * Redis Primary-Replica 설정
 * <p>
 * 쓰기는 Primary, 읽기는 Replica에서 처리하는 구조입니다.
 * Lua Script를 통해 원자성을 보장합니다.
 *
 * <h3>구조:</h3>
 * <pre>
 * ┌──────────────┐    Replication    ┌──────────────┐
 * │   Primary    │ ─────────────────▶│   Replica    │
 * │ (Write Only) │                   │ (Read Only)  │
 * │  localhost   │                   │  localhost   │
 * │    :6379     │                   │    :6380     │
 * └──────────────┘                   └──────────────┘
 * </pre>
 *
 * <h3>Spring Data Redis 4.0 변경사항:</h3>
 * <ul>
 *   <li>GenericJackson2JsonRedisSerializer → GenericJacksonJsonRedisSerializer (Jackson 3)</li>
 *   <li>GenericObjectPoolConfig 타입 파라미터 필수</li>
 * </ul>
 *
 * <h3>Lua Script 로딩:</h3>
 * <ul>
 *   <li>refresh_token_save.lua - 토큰 저장 (중복 처리)</li>
 *   <li>refresh_token_revoke.lua - 토큰 폐기</li>
 *   <li>login_attempt.lua - 로그인 시도 카운팅</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class RedisPrimaryReplicaConfig {

  private final RedisProperties redisProperties;

  /**
   * Primary-Replica 연결 팩토리
   * <p>
   * 쓰기: Primary
   * 읽기: Replica 우선, 실패 시 Primary
   * </p>
   */
  @Bean
  @Primary
  public LettuceConnectionFactory redisConnectionFactory() {
    // Primary 노드 설정
    RedisStaticMasterReplicaConfiguration config =
        new RedisStaticMasterReplicaConfiguration(
            redisProperties.getPrimary().getHost(),
            redisProperties.getPrimary().getPort()
        );

    // Replica 노드 추가 (설정된 경우)
    if (redisProperties.hasReplica()) {
      config.addNode(
          redisProperties.getReplica().getHost(),
          redisProperties.getReplica().getPort()
      );
      log.info("Redis Replica 노드 추가: {}", redisProperties.getReplicaAddress());
    }

    // 비밀번호 설정
    if (redisProperties.hasPassword()) {
      config.setPassword(RedisPassword.of(redisProperties.getPassword()));
    }

    // 커넥션 풀 설정 (Spring Data Redis 4.0: 타입 파라미터 필수)
    GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(redisProperties.getPool().getMaxActive());
    poolConfig.setMaxIdle(redisProperties.getPool().getMaxIdle());
    poolConfig.setMinIdle(redisProperties.getPool().getMinIdle());
    poolConfig.setMaxWait(Duration.ofMillis(redisProperties.getPool().getMaxWait()));

    // Lettuce 클라이언트 설정 - 읽기는 Replica 우선
    LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        .poolConfig(poolConfig)
        .commandTimeout(Duration.ofMillis(redisProperties.getTimeout()))
        .readFrom(redisProperties.hasReplica() ? ReadFrom.REPLICA_PREFERRED : ReadFrom.MASTER)
        .build();

    log.info("Redis 연결 설정 완료 - Primary: {}, Replica: {}",
        redisProperties.getPrimaryAddress(),
        redisProperties.hasReplica() ? redisProperties.getReplicaAddress() : "없음");

    return new LettuceConnectionFactory(config, clientConfig);
  }

  /**
   * RedisTemplate 설정
   * <p>
   * Spring Data Redis 4.0: GenericJacksonJsonRedisSerializer (Jackson 3 기반) 사용
   * </p>
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key: String
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Value: JSON (Jackson 3 기반 - Spring Data Redis 4.0)
    GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
        .enableSpringCacheNullValueSupport()
        .build();
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }

  /**
   * String 전용 RedisTemplate (Lua Script용)
   */
  @Bean
  public RedisTemplate<String, String> stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setValueSerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setHashValueSerializer(stringSerializer);

    template.afterPropertiesSet();
    return template;
  }

  // ========================================
  // Lua Scripts
  // ========================================

  /**
   * RefreshToken 저장 Lua Script
   * <p>
   * 원자적으로 토큰 저장 및 중복 처리
   * </p>
   */
  @Bean
  public DefaultRedisScript<String> refreshTokenSaveScript() {
    DefaultRedisScript<String> script = new DefaultRedisScript<>();
    script.setScriptSource(new ResourceScriptSource(
        new ClassPathResource("lua/refresh_token_save.lua")));
    script.setResultType(String.class);
    return script;
  }

  /**
   * RefreshToken 폐기 Lua Script
   */
  @Bean
  public DefaultRedisScript<Long> refreshTokenRevokeScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(new ResourceScriptSource(
        new ClassPathResource("lua/refresh_token_revoke.lua")));
    script.setResultType(Long.class);
    return script;
  }

  /**
   * 전체 로그아웃 Lua Script
   */
  @Bean
  public DefaultRedisScript<Long> refreshTokenRevokeAllScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(new ResourceScriptSource(
        new ClassPathResource("lua/refresh_token_revoke_all.lua")));
    script.setResultType(Long.class);
    return script;
  }

  /**
   * 로그인 시도 카운팅 Lua Script
   */
  @Bean
  public DefaultRedisScript<String> loginAttemptScript() {
    DefaultRedisScript<String> script = new DefaultRedisScript<>();
    script.setScriptSource(new ResourceScriptSource(
        new ClassPathResource("lua/login_attempt.lua")));
    script.setResultType(String.class);
    return script;
  }
}