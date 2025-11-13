# Feature 010: REST API Integration Tests with Docker-Based TestContainers

**Status**: Planned
**Branch**: TBD (to be created)
**Depends On**: Feature 009 (merged to main)

## Overview

Implement comprehensive integration tests for the REST API layer using TestContainers with Docker, ensuring full end-to-end testing of the REST → Kafka → Services workflow without requiring external infrastructure.

## Background

Feature 009 implemented the REST API with unit tests that mock service dependencies. However, integration tests that verify the complete workflow are currently failing due to missing Docker environment:

**Current State**:
- ✅ Unit tests: All 4 SearchControllerTest tests passing (with mocks)
- ❌ Integration tests: Failing due to missing Docker/TestContainers setup
- ❌ Repository integration tests: 5 tests failing (FundingProgram, Organization, Domain, AdminUser, SearchResult)
- ❌ Kafka integration test: 1 test failing (SearchRequestConsumerIntegrationTest)

**Impact**:
- Cannot verify full workflow in tests (REST → Kafka → Database)
- Cannot test actual Kafka message publication/consumption
- Cannot verify database persistence from REST endpoints
- Manual testing required for workflow validation

## Goals

1. **Configure Docker Environment** for TestContainers
2. **Implement REST API Integration Tests** with real Kafka and PostgreSQL
3. **Fix Existing Repository Integration Tests** (migrate to TestContainers if needed)
4. **Add End-to-End Workflow Tests** covering complete search execution flow
5. **Document Testing Strategy** and test execution requirements

## Success Criteria

- [ ] All integration tests pass locally with Docker
- [ ] TestContainers configuration documented
- [ ] End-to-end workflow test demonstrates: REST → Kafka → Consumer → Database
- [ ] No mocked dependencies in integration tests (real Kafka, real PostgreSQL)
- [ ] Integration tests run in CI/CD pipeline
- [ ] Clear separation between unit tests (fast, no Docker) and integration tests (slower, requires Docker)

## Technical Approach

### 1. Docker Environment Setup

**Option A: Local Docker Desktop**
- Install Docker Desktop for macOS
- Configure TestContainers to use local Docker
- **Pros**: Standard approach, well-documented
- **Cons**: Resource intensive on MacBook M2

**Option B: Remote Docker on Mac Studio**
- Configure TestContainers to use Docker on Mac Studio @ 192.168.1.10:2375
- Set `DOCKER_HOST=tcp://192.168.1.10:2375`
- **Pros**: Offloads resource usage to Mac Studio
- **Cons**: Network dependency, requires secure Docker API access

**Option C: TestContainers Cloud**
- Use TestContainers Cloud service for remote containers
- **Pros**: No local Docker required
- **Cons**: External dependency, potential cost

**Recommendation**: Start with Option B (remote Docker on Mac Studio), fallback to Option A if issues.

### 2. Integration Test Structure

```
northstar-rest-api/
└── src/test/java/
    ├── unit/                          # Fast unit tests (mocks)
    │   └── SearchControllerTest.java  # Existing - 4 tests
    └── integration/                   # Slow integration tests (Docker)
        ├── SearchWorkflowIntegrationTest.java    # NEW - End-to-end
        ├── KafkaIntegrationTest.java              # NEW - Kafka message flow
        └── DatabasePersistenceIntegrationTest.java # NEW - DB writes
```

### 3. TestContainers Configuration

**Dependencies** (already in pom.xml from Feature 009):
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.21.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.21.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.21.3</version>
    <scope>test</scope>
</dependency>
```

**Test Configuration** (application-integration-test.yml):
```yaml
spring:
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
  datasource:
    url: ${spring.datasource.url}  # Dynamic from TestContainers
    username: ${spring.datasource.username}
    password: ${spring.datasource.password}
```

### 4. Test Scenarios

#### Test 1: End-to-End Search Workflow
**Scenario**: REST request → Kafka events → Consumer processing → Database persistence

**Steps**:
1. Start TestContainers: PostgreSQL + Kafka
2. POST /api/search/execute with valid request
3. Verify response: 200 OK, sessionId returned
4. Consume Kafka messages from search-requests topic
5. Verify 3 SearchRequestEvent messages published
6. Verify DiscoverySession created in PostgreSQL
7. Verify session status = RUNNING

**Assertions**:
- HTTP 200 response
- Valid sessionId (UUID format)
- Kafka topic contains 3 messages
- Database has 1 session record
- Session metadata matches request parameters

#### Test 2: Kafka Message Structure
**Scenario**: Verify SearchRequestEvent structure and content

**Steps**:
1. POST /api/search/execute
2. Consume Kafka messages
3. Deserialize SearchRequestEvent
4. Verify all required fields present

**Assertions**:
- sessionId matches response
- query field contains generated query string
- maxResults matches request parameter
- Timestamp is recent (within 5 seconds)
- No null fields

#### Test 3: Database Persistence
**Scenario**: Verify session metadata stored correctly

**Steps**:
1. POST /api/search/execute
2. Query PostgreSQL for DiscoverySession by sessionId
3. Verify all fields persisted

**Assertions**:
- Session found in database
- status = RUNNING
- sessionType = MANUAL
- createdAt timestamp is recent
- No missing required fields

#### Test 4: Error Handling
**Scenario**: Verify graceful handling of invalid requests

**Steps**:
1. POST /api/search/execute with missing required field
2. Verify 400 Bad Request response
3. Verify no Kafka messages published
4. Verify no database records created

**Assertions**:
- HTTP 400 response
- Validation error message clear
- No side effects (no Kafka events, no DB writes)

#### Test 5: Concurrent Request Handling
**Scenario**: Verify system handles multiple simultaneous requests

**Steps**:
1. POST /api/search/execute from 5 threads concurrently
2. Verify all 5 return 200 OK
3. Verify 5 unique sessionIds
4. Verify 15 Kafka messages (3 per request)
5. Verify 5 database sessions

**Assertions**:
- All responses successful
- No sessionId collisions
- Correct Kafka message count
- Database sessions match requests

### 5. Repository Integration Test Fixes

**Current Failures**:
- `FundingProgramRepositoryTest`
- `OrganizationRepositoryTest`
- `DomainRepositoryTest`
- `AdminUserRepositoryTest`
- `SearchResultRepositoryTest`

**Root Cause**: Tests expect Docker but `DOCKER_HOST` not configured

**Fix Strategy**:
1. Add `@Testcontainers` annotation
2. Use PostgreSQLContainer with dynamic port
3. Configure datasource from container
4. Apply Flyway migrations before tests
5. Clean up containers after tests

**Example Pattern**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
public class DomainRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Tests...
}
```

## Maven Test Execution Strategy

**Unit Tests Only** (fast, no Docker required):
```bash
mvn test -Dtest='!*IntegrationTest'
```

**Integration Tests Only** (slow, requires Docker):
```bash
mvn test -Dtest='*IntegrationTest'
```

**All Tests** (CI/CD pipeline):
```bash
mvn verify
```

## Docker Setup on Mac Studio

**Enable Docker Remote API**:
```bash
# On Mac Studio @ 192.168.1.10
sudo vim /Library/LaunchDaemons/com.docker.dockerd.plist
# Add: -H tcp://0.0.0.0:2375
sudo launchctl unload /Library/LaunchDaemons/com.docker.dockerd.plist
sudo launchctl load /Library/LaunchDaemons/com.docker.dockerd.plist
```

**Configure TestContainers** (on MacBook M2):
```bash
# ~/.testcontainers.properties
docker.host=tcp://192.168.1.10:2375
```

## Implementation Phases

### Phase 1: Docker Environment Setup (1-2 hours)
- [ ] Configure remote Docker on Mac Studio OR install Docker Desktop
- [ ] Test Docker connectivity from MacBook M2
- [ ] Configure ~/.testcontainers.properties
- [ ] Verify TestContainers can start containers

### Phase 2: REST API Integration Tests (2-3 hours)
- [ ] Create integration test package structure
- [ ] Implement SearchWorkflowIntegrationTest (Test 1)
- [ ] Implement KafkaIntegrationTest (Test 2)
- [ ] Implement DatabasePersistenceIntegrationTest (Test 3)
- [ ] Implement error handling tests (Test 4)
- [ ] Implement concurrent request tests (Test 5)

### Phase 3: Repository Integration Test Fixes (1-2 hours)
- [ ] Fix FundingProgramRepositoryTest
- [ ] Fix OrganizationRepositoryTest
- [ ] Fix DomainRepositoryTest
- [ ] Fix AdminUserRepositoryTest
- [ ] Fix SearchResultRepositoryTest

### Phase 4: CI/CD Integration (1 hour)
- [ ] Document test execution strategy
- [ ] Update GitHub Actions workflow (if exists)
- [ ] Add Docker requirement to README
- [ ] Document troubleshooting steps

### Phase 5: Documentation (30 minutes)
- [ ] Update CLAUDE.md with integration test patterns
- [ ] Document Docker setup requirements
- [ ] Add testing strategy ADR (if needed)
- [ ] Update session summary

## Estimated Effort

**Total**: 5-8 hours

**Breakdown**:
- Docker setup: 1-2 hours
- REST API tests: 2-3 hours
- Repository test fixes: 1-2 hours
- CI/CD integration: 1 hour
- Documentation: 30 minutes

## Dependencies

**External**:
- Docker (Mac Studio remote OR Docker Desktop local)
- PostgreSQL 16 image (pulled by TestContainers)
- Kafka image (pulled by TestContainers)

**Internal**:
- Feature 009 merged to main
- All Feature 009 modules available

## Risks & Mitigation

**Risk 1**: Remote Docker on Mac Studio network latency
- **Mitigation**: Fallback to local Docker Desktop if performance issues

**Risk 2**: TestContainers startup time (slow tests)
- **Mitigation**: Use singleton containers pattern to reuse across tests

**Risk 3**: Docker storage space on Mac Studio
- **Mitigation**: Configure Docker to clean up old containers/images

**Risk 4**: Port conflicts on Mac Studio
- **Mitigation**: Use dynamic ports from TestContainers

## Success Metrics

- All 6+ integration tests passing
- Test execution time < 30 seconds per integration test
- 100% code coverage for SearchController (unit + integration)
- Zero mocked dependencies in integration tests
- Repeatable test execution (no flaky tests)

## Out of Scope

- Performance testing (load testing, stress testing)
- Security testing (penetration testing, vulnerability scanning)
- UI/frontend testing
- Consumer integration tests (separate from REST API focus)
- Kafka performance tuning

## References

- TestContainers documentation: https://www.testcontainers.org/
- Spring Boot TestContainers: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers
- ADR 003: TestContainers Integration Test Pattern (northstar-notes/decisions/003-testcontainers-integration-test-pattern.md)
- Feature 009 Implementation (commits 105d4a5, 984219d, 91ca0ac, 6fe1ad5)
