package com.jun_bank.auth_server.support.config;

import com.jun_bank.common_lib.event.IntegrationEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 테스트용 Kafka 설정
 * <p>
 * 실제 Kafka 연결 없이 Mock KafkaTemplate을 제공합니다.
 * <p>
 * 두 가지 타입의 KafkaTemplate을 제공:
 * <ul>
 *   <li>KafkaTemplate&lt;String, IntegrationEvent&gt; - KafkaProducerConfig 대체</li>
 *   <li>KafkaTemplate&lt;String, Object&gt; - EventRetryService용</li>
 * </ul>
 *
 * <h3>테스트에서 사용:</h3>
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestKafkaConfig.class)
 * class MyTest {
 *     // Kafka 메시지 발송이 Mock 처리됨
 * }
 * }</pre>
 */
@TestConfiguration
public class TestKafkaConfig {

    // ========================================
    // IntegrationEvent용 (KafkaProducerConfig 대체)
    // ========================================

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ProducerFactory<String, IntegrationEvent> producerFactory() {
        return Mockito.mock(ProducerFactory.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, IntegrationEvent> kafkaTemplate() {
        KafkaTemplate<String, IntegrationEvent> mockTemplate = Mockito.mock(KafkaTemplate.class);
        setupMockKafkaTemplate(mockTemplate);
        return mockTemplate;
    }

    // ========================================
    // Object용 (범용)
    // ========================================

    @Bean
    @SuppressWarnings("unchecked")
    public ProducerFactory<String, Object> objectProducerFactory() {
        return Mockito.mock(ProducerFactory.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> objectKafkaTemplate() {
        KafkaTemplate<String, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);
        setupMockKafkaTemplate(mockTemplate);
        return mockTemplate;
    }

    // ========================================
    // 공통 Mock 설정
    // ========================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setupMockKafkaTemplate(KafkaTemplate mockTemplate) {
        CompletableFuture future = new CompletableFuture<>();
        SendResult sendResult = Mockito.mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test-topic", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);

        when(mockTemplate.send(anyString(), any())).thenReturn(future);
        when(mockTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(mockTemplate.send(any(ProducerRecord.class))).thenReturn(future);
    }
}