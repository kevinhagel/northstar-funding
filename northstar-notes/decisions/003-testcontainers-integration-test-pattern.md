# ADR 003: TestContainers Integration Test Pattern

**Status**: ✅ IMPLEMENTED (Feature 003 - 125 integration tests)
**Date**: 2025-10-31
**Updated**: 2025-11-02
**Context Tags**: #architecture #testing #testcontainers #spring-boot #implemented

## Context

**CURRENT PROJECT STATE** (Updated 2025-11-02): Domain model, persistence layer, and crawler infrastructure complete. Unit tests use Mockito (300 tests). TestContainers integration tests IMPLEMENTED with 125 tests across 2 modules.

This ADR documents the **implemented pattern** for TestContainers integration tests, validated through Feature 003 implementation.

When writing integration tests for Spring Data JDBC repositories with TestContainers, there is a critical choice between two approaches:

1. **@DataJdbcTest** - Sliced test that only loads JDBC components
2. **@SpringBootTest** - Full application context test

## Decision

**Use @DataJdbcTest for individual repository tests. Use @SpringBootTest for full integration/workflow tests.**

### Repository Tests (One per Repository)
- **Each repository gets its own test class** (e.g., `DomainRepositoryTest`, `OrganizationRepositoryTest`)
- **Use @DataJdbcTest** - Loads only JDBC slice, faster startup
- **Pattern**: See `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchQueryRepositoryTest.java`

### Integration/Workflow Tests (Multi-component)
- **Use @SpringBootTest** - Full application context with all services
- **Pattern**: See `backend/src/test/java/com/northstar/funding/integration/DiscoveryWorkflowIntegrationTest.java`

## Correct Patterns

### Pattern 1: Repository Test (@DataJdbcTest)

**Use for**: Testing individual repository methods, JDBC queries, database constraints

```java
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DomainRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DomainRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSaveAndFindByDomainName() {
        // Test implementation
    }
}
```

### Pattern 2: Integration/Workflow Test (@SpringBootTest)

**Use for**: Testing workflows, services, multi-component interactions

```java
@SpringBootTest
@Testcontainers
@Transactional
class DiscoveryWorkflowIntegrationTest {

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

    @Autowired
    private DiscoveryOrchestrationService orchestrationService;

    @Autowired
    private FundingSourceCandidateRepository candidateRepository;

    @Test
    void shouldCompleteFullWorkflow() {
        // Test implementation
    }
}
```

## Required Annotations

### For Repository Tests (@DataJdbcTest)
1. **@DataJdbcTest** - Loads only JDBC slice (faster than @SpringBootTest)
2. **@Testcontainers** - Enables TestContainers lifecycle management
3. **@ActiveProfiles("postgres-test")** - Uses postgres-test profile
4. **@AutoConfigureTestDatabase(replace = NONE)** - Use TestContainers instead of embedded DB
5. **@Container** - Marks PostgreSQL container for lifecycle management
6. **@DynamicPropertySource** - Overrides datasource properties for test container
7. **@BeforeEach** - Clean up repository before each test (repository.deleteAll())

### For Integration Tests (@SpringBootTest)
1. **@SpringBootTest** - Loads full Spring Boot application context
2. **@Testcontainers** - Enables TestContainers lifecycle management
3. **@Transactional** - Automatic rollback after each test
4. **@Container** - Marks PostgreSQL container for lifecycle management
5. **@DynamicPropertySource** - Overrides datasource properties for test container

## DO NOT Use

### ❌ WRONG - Single Monolithic Test for All Repositories

```java
// NEVER DO THIS - One test class testing multiple repositories
@DataJdbcTest
public class PersistenceIntegrationTest {
    @Autowired private DomainRepository domainRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private FundingProgramRepository fundingProgramRepository;

    @Test void testDomainRepository() { ... }
    @Test void testOrganizationRepository() { ... }
    @Test void testFundingProgramRepository() { ... }
}
```

### Why One Test Per Repository

1. **Focused Testing**: Each repository test focuses on one repository's methods
2. **Clear Failures**: When tests fail, immediately know which repository has issues
3. **Better Organization**: Easier to find and maintain tests
4. **Follows Existing Pattern**: Matches `SearchQueryRepositoryTest`, `SearchSessionStatisticsRepositoryTest`

## Consequences

### Positive
- Uses proven pattern from existing codebase
- Full application context ensures all beans are wired correctly
- Flyway migrations run automatically
- @Transactional rollback keeps tests isolated
- No manual configuration needed

### Negative
- Slightly slower test startup (full context vs sliced)
- May load unnecessary beans for simple repository tests

### Neutral
- Consistent with all existing integration tests in the project

## Reference Implementations

**Status**: ✅ IMPLEMENTED (2025-11-02)

### Repository Test Examples (Use @DataJdbcTest) - IMPLEMENTED:
- `northstar-persistence/src/test/java/.../repository/AdminUserRepositoryTest.java` (14 tests)
- `northstar-persistence/src/test/java/.../repository/DomainRepositoryTest.java` (15 tests)
- `northstar-persistence/src/test/java/.../repository/OrganizationRepositoryTest.java` (11 tests)
- `northstar-persistence/src/test/java/.../repository/FundingProgramRepositoryTest.java` (14 tests)
- `northstar-persistence/src/test/java/.../repository/SearchResultRepositoryTest.java` (13 tests)

**Total**: 67 repository integration tests using @DataJdbcTest

### Integration Test Examples (Use @SpringBootTest) - IMPLEMENTED:
- `northstar-crawler/src/test/java/.../integration/DomainDeduplicationTest.java` (6 tests)
- `northstar-crawler/src/test/java/.../integration/AntiSpamIntegrationTest.java` (7 tests)
- `northstar-crawler/src/test/java/.../integration/MultiProviderSearchTest.java` (6 tests)
- `northstar-crawler/src/test/java/.../integration/SingleProviderSearchTest.java` (6 tests)
- `northstar-crawler/src/test/java/.../integration/RateLimitingTest.java` (7 tests)
- `northstar-crawler/src/test/java/.../integration/TimeoutHandlingTest.java` (7 tests)
- `northstar-crawler/src/test/java/.../integration/PartialFailureHandlingTest.java` (3 tests)
- `northstar-crawler/src/test/java/.../integration/WeeklySimulationTest.java` (6 tests)
- `northstar-crawler/src/test/java/.../integration/ManualValidationTest.java` (11 tests)

**Total**: 58 crawler integration tests using @SpringBootTest

**Summary**: 125 total integration tests (67 repository + 58 crawler workflow)
**Current Testing**: 460 total tests = 300 unit tests + 35 contract tests + 125 integration tests

## Enforcement

**MANDATORY RULES**:

### For Repository Tests:
1. **One test class per repository** (e.g., `DomainRepositoryTest` for `DomainRepository`)
2. Use @DataJdbcTest annotation
3. Copy pattern from `SearchQueryRepositoryTest.java`
4. Include @BeforeEach with repository.deleteAll()
5. Test all custom finder methods and queries

### For Integration/Workflow Tests:
1. Use @SpringBootTest annotation
2. Copy pattern from `DiscoveryWorkflowIntegrationTest.java`
3. Add @Transactional for automatic cleanup
4. Test multi-component workflows and service interactions

**Never create monolithic tests that test multiple repositories in one class.**

## Implementation Status

**Status**: ✅ FULLY IMPLEMENTED (2025-11-02)

**Persistence Layer** (northstar-persistence):
- 67 repository integration tests using @DataJdbcTest + TestContainers
- 135 service unit tests using Mockito
- Pattern validated and working

**Crawler Layer** (northstar-crawler):
- 58 integration tests using @SpringBootTest + TestContainers
- 165 unit tests using Mockito + WireMock
- 35 contract tests for interface compliance
- Pattern validated across complex multi-provider orchestration

**Total Test Coverage**: 460 tests (all passing)
- Unit: 300 tests
- Contract: 35 tests
- Integration: 125 tests

## Related Documentation

- [[002-domain-level-deduplication]] - Domain model design
- `CLAUDE.md` - TestContainers Best Practices section (to be added when implementing)
