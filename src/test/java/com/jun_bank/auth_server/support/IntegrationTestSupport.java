package com.jun_bank.auth_server.support;

import com.jun_bank.auth_server.support.config.TestKafkaConfig;
import com.jun_bank.auth_server.support.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 기반 클래스
 * <p>
 * Testcontainers로 Redis를 띄우고, H2 인메모리 DB를 사용합니다.
 * Adapter 통합 테스트에서 상속받아 사용합니다.
 *
 * <h3>제공 기능:</h3>
 * <ul>
 *   <li>Redis Testcontainer 자동 시작 (싱글톤 패턴)</li>
 *   <li>H2 인메모리 DB 설정</li>
 *   <li>테스트용 Security 설정</li>
 *   <li>테스트용 Kafka Mock 설정</li>
 * </ul>
 *
 * <h3>사용법:</h3>
 * <pre>{@code
 * @SpringBootTest
 * class MyAdapterTest extends IntegrationTestSupport {
 *     // Redis + H2 + Security + Kafka 자동 설정
 * }
 * }</pre>
 *
 * <h3>싱글톤 컨테이너 패턴:</h3>
 * <p>
 * 모든 테스트 클래스에서 동일한 Redis 컨테이너를 공유합니다.
 * JVM 종료 시 자동으로 컨테이너가 정리됩니다.
 * </p>
 */
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestKafkaConfig.class})
public abstract class IntegrationTestSupport {

    private static final int REDIS_PORT = 6379;

    // 싱글톤 컨테이너 - static 블록에서 한 번만 시작
    static final GenericContainer<?> REDIS;

    static {
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(REDIS_PORT);
        REDIS.start();

        // JVM 종료 시 컨테이너 정리
        Runtime.getRuntime().addShutdownHook(new Thread(REDIS::stop));

        System.out.println("==============================================");
        System.out.println("Redis 컨테이너 시작됨 - Port: " + REDIS.getMappedPort(REDIS_PORT));
        System.out.println("==============================================");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis 설정 - 싱글톤 컨테이너 사용
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("redis.primary.host", REDIS::getHost);
        registry.add("redis.primary.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("redis.replica.host", REDIS::getHost);
        registry.add("redis.replica.port", () -> REDIS.getMappedPort(REDIS_PORT));

        // H2 인메모리 DB 설정
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");

        // Eureka, Config Server 비활성화 (테스트용)
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }
}