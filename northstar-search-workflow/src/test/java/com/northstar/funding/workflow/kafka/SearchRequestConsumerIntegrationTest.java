package com.northstar.funding.workflow.kafka;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.kafka.events.SearchRequestEvent;
import com.northstar.funding.kafka.events.SearchResultEvent;
import com.northstar.funding.search.adapter.SearchAdapter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for SearchRequestConsumer using Testcontainers.
 *
 * <p>Pattern follows northstar-persistence repository tests:
 * <ul>
 *   <li>@SpringBootTest for full application context</li>
 *   <li>@Testcontainers with static @Container fields</li>
 *   <li>@DynamicPropertySource for container configuration</li>
 *   <li>@ActiveProfiles for test-specific configuration</li>
 * </ul>
 *
 * <p>Tests real Kafka message flow:
 * search-requests → SearchRequestConsumer → search-results-raw
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("kafka-test")
class SearchRequestConsumerIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        private static SearchAdapter mockAdapter;

        @Bean
        public List<SearchAdapter> searchAdapters() {
            mockAdapter = mock(SearchAdapter.class);
            List<SearchAdapter> adapters = new ArrayList<>();
            adapters.add(mockAdapter);
            return adapters;
        }

        @Bean
        public RedisTemplate<String, Boolean> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Boolean> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericToStringSerializer<>(Boolean.class));
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericToStringSerializer<>(Boolean.class));
            template.afterPropertiesSet();
            return template;
        }

        public static SearchAdapter getMockAdapter() {
            return mockAdapter;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @Container
    static GenericContainer<?> valkey = new GenericContainer<>(
            DockerImageName.parse("valkey/valkey:7.2-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Valkey (Redis)
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", valkey::getFirstMappedPort);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaConsumer<String, SearchResultEvent> testConsumer;

    @BeforeEach
    void setUp() {
        // Reset mock adapter stubbing between tests
        SearchAdapter adapter = TestConfig.getMockAdapter();
        Mockito.reset(adapter);

        // Create test consumer for search-results-raw topic
        testConsumer = createTestConsumer("search-results-raw");
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    void shouldPublishSearchResultsToRawTopic() {
        // Given - mock search adapter returns results
        SearchAdapter adapter = TestConfig.getMockAdapter();
        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.getEngineType()).thenReturn(SearchEngineType.SEARXNG);
        when(adapter.search(anyString(), anyInt()))
                .thenReturn(List.of(
                        new SearchAdapter.SearchResult(
                                "https://education.gov.bg/grants",
                                "Bulgarian Education Grants",
                                "Apply for education grants"
                        )
                ));

        UUID sessionId = UUID.randomUUID();
        SearchRequestEvent request = SearchRequestEvent.builder()
                .sessionId(sessionId)
                .query("Bulgaria education grants")
                .maxResults(25)
                .build();

        // When - publish search request to Kafka
        try {
            kafkaTemplate.send("search-requests", request).get();  // Wait for send to complete
        } catch (Exception e) {
            throw new RuntimeException("Failed to send search request", e);
        }

        // Then - verify result published to search-results-raw
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var records = testConsumer.poll(Duration.ofMillis(100));
                    assertThat(records).isNotEmpty();

                    SearchResultEvent result = records.iterator().next().value();
                    assertThat(result.getSessionId()).isEqualTo(sessionId);
                    assertThat(result.getUrl()).isEqualTo("https://education.gov.bg/grants");
                    assertThat(result.getTitle()).isEqualTo("Bulgarian Education Grants");
                });
    }

    @Test
    void shouldHandleAdapterExceptionsGracefully() {
        // Given - adapter throws exception
        SearchAdapter adapter = TestConfig.getMockAdapter();
        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.search(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Search API timeout"));

        UUID sessionId = UUID.randomUUID();
        SearchRequestEvent request = SearchRequestEvent.builder()
                .sessionId(sessionId)
                .query("test query")
                .maxResults(10)
                .build();

        // When - publish search request
        try {
            kafkaTemplate.send("search-requests", request).get();  // Wait for send to complete
        } catch (Exception e) {
            throw new RuntimeException("Failed to send search request", e);
        }

        // Then - no results published (error logged, message acknowledged)
        await().pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    var records = testConsumer.poll(Duration.ofMillis(100));
                    assertThat(records.isEmpty()).isTrue();
                });
    }

    private KafkaConsumer<String, SearchResultEvent> createTestConsumer(String topic) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                JsonDeserializer.TRUSTED_PACKAGES, "com.northstar.funding.kafka.events",
                JsonDeserializer.VALUE_DEFAULT_TYPE, SearchResultEvent.class.getName()
        );

        KafkaConsumer<String, SearchResultEvent> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
