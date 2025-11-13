# Research: Docker-Based Integration Tests

**Feature**: 011-create-comprehensive-docker
**Date**: 2025-11-13
**Status**: Complete

## Research Overview

This document consolidates research findings for implementing Docker-based integration tests using TestContainers for the NorthStar Funding REST API layer.

---

## 1. TestContainers Configuration with Remote Docker

### Decision
Use remote Docker on Mac Studio (192.168.1.10:2375) as primary approach, with local Docker Desktop as fallback.

### Rationale
- **Mac Studio has Docker already running**: Existing infrastructure at 192.168.1.10
- **Offloads resource usage**: MacBook M2 development machine isn't resource-constrained
- **Consistent with constitution**: Infrastructure Integration principle mandates Mac Studio for services
- **TestContainers supports remote Docker**: Via DOCKER_HOST environment variable or ~/.testcontainers.properties

### Configuration Approach
**Primary: ~/.testcontainers.properties**
```properties
docker.host=tcp://192.168.1.10:2375
testcontainers.reuse.enable=true
```

**Fallback: Local Docker Desktop**
- Remove or comment out docker.host setting
- TestContainers automatically detects local Docker

**Mac Studio Docker API Setup** (Kevin will execute):
```bash
# On Mac Studio, enable TCP socket (if not already enabled)
# Edit /Library/LaunchDaemons/com.docker.dockerd.plist
# Add -H tcp://0.0.0.0:2375 to ExecStart
```

### Alternatives Considered
1. **Local Docker Desktop only**: Rejected due to resource usage on MacBook M2
2. **TestContainers Cloud**: Rejected due to external dependency and potential cost
3. **Docker Compose for tests**: Rejected - TestContainers provides better lifecycle management

---

## 2. TestContainers Container Selection

### PostgreSQL Container

**Decision**: Use `postgres:16-alpine` image

**Rationale**:
- Matches production PostgreSQL 16 version
- Alpine variant is lightweight (smaller download, faster startup)
- Official PostgreSQL image with TestContainers support
- Flyway migrations can run against it

**Configuration**:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test")
    .withReuse(true);  // Reuse container across test classes
```

### Kafka Container

**Decision**: Use `confluentinc/cp-kafka:7.4.0` in KRaft mode (no Zookeeper)

**Rationale**:
- **Matches production**: docker-compose.yml uses Kafka 7.4.0 in KRaft mode
- **Modern Kafka**: KRaft consensus protocol (no Zookeeper dependency)
- **TestContainers support**: `org.testcontainers.kafka.ConfluentKafkaContainer` supports KRaft
- **Faster startup**: No separate Zookeeper container required
- **Future-proof**: Kafka 3.5+ removed Zookeeper entirely

**Configuration**:
```java
@Container
protected static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
).withReuse(true);
```

**Production Configuration** (from docker-compose.yml):
```yaml
KAFKA_PROCESS_ROLES: broker,controller
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
# No ZOOKEEPER variables - pure KRaft mode
```

**Alternative**: `@EmbeddedKafka` (Spring Kafka Test)
- **Current approach** (Feature 009): Already using @EmbeddedKafka for unit tests
- **Keep for unit tests**: Fast, no Docker required
- **Use TestContainers KRaft Kafka for integration tests**: Real Kafka behavior matching production

### Ollama Container (AI Service)

**Decision**: **Do NOT containerize Ollama for tests** - use mock or stub

**Rationale**:
- **Ollama runs natively on Mac Studio**: NOT in Docker (Metal GPU acceleration)
- **Integration tests should mock Ollama**: QueryGenerationService is already tested in Feature 004 (58 tests passing)
- **Focus on REST → Kafka → Database flow**: AI query generation is separate concern
- **Test isolation**: Ollama availability shouldn't block integration tests

**Mocking Approach**:
```java
@MockBean
private QueryGenerationService queryGenerationService;

// In test setup
when(queryGenerationService.generateQueries(any()))
    .thenReturn(CompletableFuture.completedFuture(
        QueryGenerationResponse.builder()
            .queries(List.of("test query 1", "test query 2", "test query 3"))
            .build()
    ));
```

**Alternative if real Ollama needed** (future enhancement):
- Connect to Mac Studio Ollama (http://192.168.1.10:11434) via network
- Not needed for current testing scope

---

## 3. TestContainers Lifecycle Management

### Decision
Use **singleton container pattern with @Container(reuse = true)**

**Rationale**:
- **Faster test execution**: Container starts once, reused across test classes
- **Resource efficient**: Avoid repeated container startup overhead (15-30 seconds per container)
- **TestContainers best practice**: Documented pattern for Spring Boot tests
- **Acceptable risk**: Tests clean their own data (transactional rollback)

**Implementation Pattern**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:latest")
    ).withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

**Test Classes Extend Base**:
```java
public class SearchWorkflowIntegrationTest extends AbstractIntegrationTest {
    // Tests inherit container configuration
}
```

### Alternatives Considered
1. **Per-test container lifecycle**: Rejected - too slow (30s startup per test class)
2. **Static containers without reuse flag**: Rejected - containers restart between runs
3. **Docker Compose**: Rejected - manual lifecycle management, less Spring Boot integration

---

## 4. Test Data Management

### Decision
Use **@Transactional on test classes with rollback** + **Flyway migrations**

**Rationale**:
- **Spring Boot default**: @Transactional tests roll back after each test
- **Clean slate**: Each test starts with empty tables (after Flyway migrations)
- **No test interference**: Rollback prevents data pollution
- **Matches repository tests**: Existing pattern in northstar-persistence tests

**Implementation**:
```java
@SpringBootTest
@Testcontainers
@Transactional  // Rollback after each test
@ActiveProfiles("integration-test")
public class SearchWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void testSuccessfulSearchRequest() {
        // Given - test data
        // When - execute test
        // Then - verify results
        // Automatic rollback after test
    }
}
```

**Flyway in Tests**:
- Application migrations run automatically on container startup
- TestContainers starts fresh container → Flyway creates schema → Tests run → Rollback

### Alternatives Considered
1. **Manual cleanup in @AfterEach**: Rejected - error-prone, easy to miss cleanup
2. **Separate test database**: Rejected - TestContainers provides isolation
3. **SQL scripts in @Sql**: Rejected - Flyway migrations are source of truth

---

## 5. Test Execution Performance

### Decision
Target **<5 minutes total test execution time** for full integration test suite

**Rationale**:
- Resolved from spec clarification (FR-015)
- Acceptable for developer workflow (run during coffee break)
- CI/CD acceptable (not a bottleneck)
- With container reuse: ~30s startup once, then fast test execution

**Performance Optimization Strategies**:
1. **Container reuse** (@Container with reuse=true)
2. **Parallel test execution** (Maven Surefire parallel)
3. **Efficient test data** (minimal records, no unnecessary fixtures)
4. **Targeted assertions** (verify only what's needed)

**Monitoring**:
- Maven Surefire reports test execution times
- Identify slow tests for optimization
- Acceptable targets:
  - Container startup: <30 seconds (one-time)
  - Per test: <10 seconds
  - Full suite (7 scenarios + 5 repository fixes): <5 minutes

---

## 6. Repository Integration Test Fixes

### Problem Analysis
**5 failing tests** in northstar-persistence module:
1. `DomainRepositoryTest`
2. `FundingProgramRepositoryTest`
3. `OrganizationRepositoryTest`
4. `AdminUserRepositoryTest`
5. `SearchResultRepositoryTest`

**Root Cause**: Tests expect Docker but DOCKER_HOST not configured

**Error Message**:
```
java.lang.IllegalStateException: Could not find a valid Docker environment.
Please see logs and check configuration
```

### Decision
Apply **AbstractIntegrationTest base class pattern** to repository tests

**Rationale**:
- Consistent with new REST API integration tests
- Provides PostgreSQL container automatically
- Minimal code changes (extends base class, remove manual setup)
- Follows TestContainers best practices

**Fix Pattern**:
```java
// BEFORE (failing)
@SpringBootTest
public class DomainRepositoryTest {
    // No TestContainers configuration
}

// AFTER (fixed)
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
public class DomainRepositoryIntegrationTest extends AbstractIntegrationTest {
    // Inherits PostgreSQL container from base class
}
```

### Migration Strategy
1. Create `AbstractIntegrationTest` base class (shared across modules)
2. Rename tests from `*Test.java` to `*IntegrationTest.java` (clarity)
3. Change tests to extend `AbstractIntegrationTest`
4. Remove manual PostgreSQL setup code (if any)
5. Verify tests pass with TestContainers

---

## 7. Maven Test Execution Strategy

### Decision
Separate **unit tests** (fast) from **integration tests** (slow) using naming convention

**Rationale**:
- Developers want fast feedback (unit tests only)
- CI/CD needs full validation (all tests)
- Maven Surefire supports pattern-based test selection
- Industry standard pattern

**Naming Convention**:
- Unit tests: `*Test.java` (e.g., `SearchControllerTest.java`)
- Integration tests: `*IntegrationTest.java` (e.g., `SearchWorkflowIntegrationTest.java`)

**Maven Commands**:
```bash
# Unit tests only (fast, no Docker)
mvn test -Dtest='!*IntegrationTest'

# Integration tests only (slow, requires Docker)
mvn test -Dtest='*IntegrationTest'

# All tests (CI/CD)
mvn verify

# Specific integration test
mvn test -Dtest=SearchWorkflowIntegrationTest
```

**Maven Configuration** (pom.xml):
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <!-- Exclude integration tests from default 'mvn test' -->
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>integration-tests</id>
            <phase>integration-test</phase>
            <goals>
                <goal>test</goal>
            </goals>
            <configuration>
                <excludes>
                    <exclude>none</exclude>
                </excludes>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                </includes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 8. Documentation Requirements

### Docker Setup Documentation

**Decision**: Create `DOCKER-SETUP.md` in project root with step-by-step instructions

**Content Structure**:
1. **Overview**: Why Docker is needed for integration tests
2. **Option 1: Remote Docker** (Mac Studio @ 192.168.1.10:2375)
   - Mac Studio configuration (Kevin executes)
   - MacBook M2 configuration (~/.testcontainers.properties)
   - Verification steps
3. **Option 2: Local Docker Desktop**
   - Installation instructions
   - Verification steps
4. **Troubleshooting**: Common issues and solutions
5. **Running Tests**: Maven commands for different test scenarios

**Location**: `/Users/kevin/github/northstar-funding/DOCKER-SETUP.md`

### Testing Strategy Documentation

**Decision**: Update `CLAUDE.md` with testing section

**Content**:
- When to use unit tests vs integration tests
- TestContainers patterns for new tests
- Abstract base class usage
- Performance expectations

---

## 9. CI/CD Integration

### Decision
**Document approach, implementation deferred** (not in Feature 011 scope)

**Rationale**:
- No existing CI/CD pipeline to integrate with (not in project yet)
- Docker availability in CI unknown (GitHub Actions? GitLab CI? Jenkins?)
- Feature 011 focuses on local development setup
- Future feature can add CI/CD integration

**Documentation Placeholder**:
```markdown
## CI/CD Integration (Future)

When CI/CD pipeline is implemented:
1. Ensure Docker available in CI environment
2. Run `mvn verify` (all tests including integration)
3. TestContainers will automatically pull images
4. Consider caching Docker images for faster builds
5. Integration tests must pass before merge
```

---

## 10. Test Assertion Library

### Decision
Use **AssertJ** for fluent assertions

**Rationale**:
- More readable than JUnit assertions
- Better error messages
- Already in Spring Boot Test dependency
- Fluent API matches modern Java style

**Example**:
```java
// JUnit (verbose)
assertEquals(3, events.size());
assertEquals("test query", events.get(0).getQuery());
assertNotNull(events.get(0).getSessionId());

// AssertJ (fluent)
assertThat(events).hasSize(3);
assertThat(events.get(0))
    .hasFieldOrProperty("query")
    .hasFieldOrPropertyWithValue("query", "test query")
    .extracting(SearchRequestEvent::getSessionId)
    .isNotNull();
```

---

## Summary of Research Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Docker Host | Remote (Mac Studio @ 192.168.1.10:2375) | Existing infrastructure, offload resources |
| PostgreSQL | postgres:16-alpine | Matches production, lightweight |
| Kafka | confluentinc/cp-kafka:latest | Built-in TestContainers support |
| Ollama | Mock in tests | Native on Mac Studio, already tested |
| Container Lifecycle | Singleton with reuse | Fast test execution |
| Test Data | @Transactional with rollback | Clean, isolated tests |
| Performance Target | <5 minutes full suite | Acceptable for dev workflow |
| Repository Tests | Extend AbstractIntegrationTest | Consistent pattern |
| Test Organization | *Test.java vs *IntegrationTest.java | Industry standard |
| Assertions | AssertJ | Fluent, readable |

---

## Next Phase

**Phase 1: Design & Contracts**
- Extract test data models (test fixtures)
- Generate test contracts (Given/When/Then scenarios)
- Create quickstart.md (how to run tests)
- Update CLAUDE.md with testing patterns
