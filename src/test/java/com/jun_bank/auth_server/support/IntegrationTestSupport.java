package com.jun_bank.auth_server.support;

import com.jun_bank.auth_server.support.config.TestKafkaConfig;
import com.jun_bank.auth_server.support.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 기반 클래스
 * <p>
 * Testcontainers로 Redis를 띄우고, H2 인메모리 DB를 사용합니다.
 * Adapter 통합 테스트에서 상속받아 사용합니다.
 *
 * <h3>제공 기능:</h3>
 * <ul>
 *   <li>Redis Testcontainer 자동 시작</li>
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
 */
@Testcontainers
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestKafkaConfig.class})
public abstract class IntegrationTestSupport {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT)
            .withReuse(true);  // 테스트 간 컨테이너 재사용 (속도 향상)

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("redis.primary.host", redis::getHost);
        registry.add("redis.primary.port", redis::getFirstMappedPort);
        registry.add("redis.replica.host", redis::getHost);
        registry.add("redis.replica.port", redis::getFirstMappedPort);

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