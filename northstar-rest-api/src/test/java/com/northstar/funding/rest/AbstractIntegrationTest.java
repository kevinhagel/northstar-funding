package com.northstar.funding.rest;

import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests using TestContainers.
 * Provides shared PostgreSQL and Kafka containers with singleton pattern for reuse.
 *
 * All integration tests should extend this class to get:
 * - PostgreSQL 16 Alpine container
 * - Confluent Kafka container
 * - Dynamic Spring Boot property configuration
 * - Container verification
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    /**
     * Mock QueryGenerationService to avoid Ollama dependency in tests.
     * Tests can configure specific behavior via when().thenReturn() in test methods.
     */
    @MockBean
    protected QueryGenerationService queryGenerationService;

    /**
     * Mock DiscoverySessionService - required by SearchController.
     * Tests that need real database persistence should override with @SpyBean.
     */
    @MockBean
    protected DiscoverySessionService discoverySessionService;

    /**
     * Singleton PostgreSQL container (reused across test classes).
     * Uses postgres:16-alpine to match production version.
     */
    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    /**
     * Singleton Kafka container (reused across test classes within same JVM).
     * Uses Confluent Platform Kafka 7.4.0 in KRaft mode (no Zookeeper).
     * Matches production docker-compose.yml configuration.
     */
    @Container
    protected static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    /**
     * Dynamically configure Spring Boot properties for TestContainers.
     * Overrides application-integration-test.yml properties with container URLs.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Flyway enabled for schema creation
        registry.add("spring.flyway.enabled", () -> "true");
    }

    /**
     * Verify containers started successfully before running tests.
     * Provides clear error messages if container startup fails.
     */
    @BeforeAll
    static void verifyContainers() {
        if (!postgres.isRunning()) {
            throw new IllegalStateException("PostgreSQL container failed to start. " +
                "Check Docker connectivity and resource availability.");
        }
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container failed to start. " +
                "Check Docker connectivity and resource availability.");
        }
    }
}
