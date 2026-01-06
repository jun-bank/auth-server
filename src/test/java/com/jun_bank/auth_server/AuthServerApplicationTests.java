package com.jun_bank.auth_server;

import com.jun_bank.auth_server.support.config.TestKafkaConfig;
import com.jun_bank.auth_server.support.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트 로드 테스트
 * <p>
 * Spring Context가 정상적으로 로드되는지 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestKafkaConfig.class})
class AuthServerApplicationTests {

	@Test
	void contextLoads() {
		// Spring Context가 정상적으로 로드되는지 확인
	}
}