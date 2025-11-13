# Data Model: Integration Test Infrastructure

**Feature**: 011-create-comprehensive-docker
**Date**: 2025-11-13
**Purpose**: Define test data structures, fixtures, and patterns for integration testing

---

## Overview

This document defines the test data models, fixtures, and patterns needed for Docker-based integration tests. Since this feature focuses on testing infrastructure (not application features), the "data model" refers to test fixtures, base classes, and testing patterns.

---

## 1. Base Test Infrastructure

### AbstractIntegrationTest (Base Class)

**Purpose**: Provide shared TestContainers configuration for all integration tests

**Location**:
- Primary: `northstar-rest-api/src/test/java/com/northstar/funding/rest/AbstractIntegrationTest.java`
- Shareable across modules if needed

**Structure**:
```java
package com.northstar.funding.rest;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    // Singleton PostgreSQL container (reused across tests)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    // Singleton Kafka container (reused across tests)
    // Uses Kafka 7.4.0 in KRaft mode (no Zookeeper) - matches production
    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    ).withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Disable Flyway for tests (use migrations from test resources)
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeAll
    static void verifyContainers() {
        // Verify containers started successfully
        if (!postgres.isRunning()) {
            throw new IllegalStateException("PostgreSQL container failed to start");
        }
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container failed to start");
        }
    }
}
```

**Key Fields**:
- `postgres`: PostgreSQL 16 Alpine container
- `kafka`: Confluent Kafka container

**Configuration**:
- Reuse enabled for performance
- Dynamic properties configured via `@DynamicPropertySource`
- Verification in `@BeforeAll` for clear error messages

**Relationships**:
- Extended by all integration test classes
- Provides containers and Spring Boot configuration

---

## 2. Test Fixtures (Request Data)

### SearchExecutionRequest Fixtures

**Purpose**: Standard test requests for REST API testing

**Location**: Test utility class or test constants

**Fixtures**:

```java
public class TestFixtures {

    public static SearchExecutionRequest validSearchRequest() {
        return new SearchExecutionRequest(
            Set.of(FundingSourceType.GOVERNMENT_EU, FundingSourceType.GOVERNMENT_NATIONAL),
            Set.of(FundingMechanism.GRANT, FundingMechanism.SCHOLARSHIP),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria", "Eastern Europe"),
            QueryLanguage.ENGLISH,
            25
        );
    }

    public static SearchExecutionRequest invalidSearchRequest_MissingFundingTypes() {
        return new SearchExecutionRequest(
            Set.of(), // EMPTY - validation error
            Set.of(FundingMechanism.GRANT),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria"),
            QueryLanguage.ENGLISH,
            25
        );
    }

    public static SearchExecutionRequest invalidSearchRequest_LowMaxResults() {
        return new SearchExecutionRequest(
            Set.of(FundingSourceType.GOVERNMENT_EU),
            Set.of(FundingMechanism.GRANT),
            ProjectScale.SMALL,
            Set.of(BeneficiaryPopulation.EDUCATORS_TEACHERS),
            Set.of(RecipientOrganizationType.UNIVERSITY_PUBLIC),
            Set.of("Bulgaria"),
            QueryLanguage.ENGLISH,
            5  // TOO LOW - min is 10
        );
    }
}
```

**Fields** (SearchExecutionRequest):
- fundingSourceTypes: Set<FundingSourceType> (NOT EMPTY)
- fundingMechanisms: Set<FundingMechanism> (NOT EMPTY)
- projectScale: ProjectScale (REQUIRED)
- beneficiaryPopulations: Set<BeneficiaryPopulation> (NOT EMPTY)
- recipientOrganizationTypes: Set<RecipientOrganizationType> (NOT EMPTY)
- geographicScope: Set<String> (NOT EMPTY)
- queryLanguage: QueryLanguage (REQUIRED)
- maxResultsPerQuery: Integer (MIN 10, MAX 100)

**Validation Rules**:
- At least one funding source type required
- At least one mechanism required
- Max results between 10-100
- All required fields must be present

---

## 3. Test Assertions (Expected Data)

### Expected Database State

**Purpose**: Define expected database records after successful request

**DiscoverySession**:
```java
public class ExpectedDatabaseState {

    public static void assertSessionCreated(UUID sessionId, JdbcTemplate jdbcTemplate) {
        String sql = "SELECT COUNT(*) FROM discovery_session WHERE session_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
        assertThat(count).isEqualTo(1);

        // Verify session metadata
        String statusSql = "SELECT status FROM discovery_session WHERE session_id = ?";
        String status = jdbcTemplate.queryForObject(statusSql, String.class, sessionId);
        assertThat(status).isEqualTo("RUNNING");
    }

    public static void assertNoSessionCreated(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT COUNT(*) FROM discovery_session";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(0);
    }
}
```

**Fields to Verify**:
- session_id: UUID (auto-generated)
- status: SessionStatus (RUNNING for new sessions)
- session_type: SessionType (MANUAL for REST API requests)
- created_at: Timestamp (recent, within 5 seconds)

**Validation Rules**:
- Exactly one session created per request
- No sessions created for invalid requests
- Session status correct for workflow stage

### Expected Kafka Events

**Purpose**: Define expected Kafka messages after successful request

**SearchRequestEvent**:
```java
public class ExpectedKafkaEvents {

    public static void assertEventsPublished(
        List<SearchRequestEvent> events,
        UUID expectedSessionId,
        int expectedCount
    ) {
        assertThat(events).hasSize(expectedCount);

        events.forEach(event -> {
            assertThat(event.getSessionId()).isEqualTo(expectedSessionId);
            assertThat(event.getQuery()).isNotBlank();
            assertThat(event.getMaxResults()).isGreaterThanOrEqualTo(10);
        });
    }

    public static void assertNoEventsPublished(List<SearchRequestEvent> events) {
        assertThat(events).isEmpty();
    }
}
```

**Fields to Verify** (SearchRequestEvent):
- sessionId: UUID (matches REST API response)
- query: String (generated query text, NOT BLANK)
- maxResults: Integer (matches request parameter)
- timestamp: Instant (recent)

**Validation Rules**:
- Exactly 3 events per successful request (3 search engines)
- All events have same sessionId
- No events for invalid requests

---

## 4. Test Scenarios (Behavioral Patterns)

### Scenario Pattern

**Given-When-Then Structure**:
```java
@Test
void testScenario() {
    // GIVEN - Setup test data and mocks
    SearchExecutionRequest request = TestFixtures.validSearchRequest();
    setupMocks();

    // WHEN - Execute action
    ResponseEntity<SearchExecutionResponse> response = executeRequest(request);

    // THEN - Verify outcomes
    assertResponse(response);
    assertDatabaseState();
    assertKafkaEvents();
}
```

### Scenario 1: Successful Search Request Flow

**Given**:
- Valid SearchExecutionRequest
- QueryGenerationService mocked (returns 3 queries)
- DiscoverySessionService mocked (returns session with UUID)

**When**:
- POST /api/search/execute

**Then**:
- HTTP 200 OK
- Response contains sessionId (UUID)
- Response contains queriesGenerated = 9 (3 engines × 3 queries)
- Database has 1 session record (status=RUNNING)
- Kafka has 9 SearchRequestEvent messages

### Scenario 2: Invalid Request Handling

**Given**:
- Invalid SearchExecutionRequest (empty fundingSourceTypes)

**When**:
- POST /api/search/execute

**Then**:
- HTTP 400 Bad Request
- Response contains validation error message
- Database has 0 session records
- Kafka has 0 messages

### Scenario 3: Database State Verification

**Given**:
- Valid request successfully processed

**When**:
- Query database for session

**Then**:
- Session exists with correct sessionId
- Session status = RUNNING
- Session type = MANUAL
- Created timestamp within last 5 seconds

### Scenario 4: Message Queue Verification

**Given**:
- Valid request successfully processed

**When**:
- Consume messages from Kafka

**Then**:
- Exactly 9 messages present
- All messages have same sessionId
- All messages have non-blank query
- All messages have correct maxResults

### Scenario 5: Concurrent Request Handling

**Given**:
- 5 valid requests submitted simultaneously

**When**:
- Execute all 5 in parallel

**Then**:
- All 5 return 200 OK
- 5 unique sessionIds
- Database has 5 session records
- Kafka has 45 messages (5 × 9)

---

## 5. Repository Test Data

### Purpose
Standard test data for repository integration tests (5 failing tests to fix)

### Domain Entity Test Data

**Location**: Test utility class

```java
public class RepositoryTestFixtures {

    public static Domain validDomain(UUID sessionId) {
        return Domain.builder()
            .domainName("example.org")
            .status(DomainStatus.DISCOVERED)
            .quality(0.75)
            .discoveredInSessionId(sessionId)
            .firstSeenAt(Instant.now())
            .build();
    }

    public static FundingProgram validFundingProgram(UUID organizationId) {
        return FundingProgram.builder()
            .organizationId(organizationId)
            .programName("Test Grant Program")
            .status(ProgramStatus.ACTIVE)
            .applicationDeadline(LocalDate.now().plusDays(30))
            .build();
    }

    public static Organization validOrganization() {
        return Organization.builder()
            .name("Test Foundation")
            .organizationConfidence(new BigDecimal("0.85"))
            .build();
    }
}
```

**Validation Rules**:
- Domain names must be unique
- Quality scores use BigDecimal (scale 2)
- All required fields present
- Relationships (foreign keys) valid

---

## 6. Test Configuration

### application-integration-test.yml

**Purpose**: Spring Boot configuration for integration tests

**Location**: `northstar-rest-api/src/test/resources/application-integration-test.yml`

**Content**:
```yaml
spring:
  # DataSource configured dynamically by TestContainers
  datasource:
    # url, username, password set by @DynamicPropertySource

  # Kafka configured dynamically by TestContainers
  kafka:
    # bootstrap-servers set by @DynamicPropertySource
    consumer:
      auto-offset-reset: earliest
      group-id: integration-test-group
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  # Flyway migrations
  flyway:
    enabled: true
    locations: classpath:db/migration

# Query generation (mock Ollama)
query-generation:
  ollama:
    base-url: http://192.168.1.10:11434/v1
    api-key: not-needed
    timeout-seconds: 60
    model-name: llama3.1:8b
  cache:
    ttl-hours: 24
    max-size: 1000

logging:
  level:
    com.northstar.funding: DEBUG
    org.testcontainers: INFO
    org.springframework.kafka: INFO
```

**Key Settings**:
- Dynamic properties for containers
- Flyway enabled for schema creation
- Kafka consumer starts at earliest offset
- Debug logging for NorthStar code

---

## Summary

This data model defines:

1. **Base Infrastructure**: `AbstractIntegrationTest` for TestContainers
2. **Test Fixtures**: Standard request objects for testing
3. **Expected Data**: Assertions for database and Kafka state
4. **Scenario Patterns**: Given-When-Then test structure
5. **Repository Data**: Fixtures for repository tests
6. **Configuration**: Spring Boot test profile

All integration tests extend `AbstractIntegrationTest` and use these patterns for consistency.
