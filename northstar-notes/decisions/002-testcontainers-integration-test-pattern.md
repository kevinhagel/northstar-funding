# ADR 002: TestContainers Integration Test Pattern

**Status**: Accepted
**Date**: 2025-10-31
**Context Tags**: #architecture #testing #testcontainers #spring-boot

## Context

When writing integration tests for Spring Data JDBC repositories with TestContainers, there is a critical choice between two approaches:

1. **@DataJdbcTest** - Sliced test that only loads JDBC components
2. **@SpringBootTest** - Full application context test

During the extraction of the persistence module, I initially attempted to create a single monolithic integration test using `@DataJdbcTest` which led to confusion about the correct testing approach.

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

### Repository Test Examples (Use @DataJdbcTest):
- `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchQueryRepositoryTest.java`
- `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchSessionStatisticsRepositoryTest.java`

### Integration Test Examples (Use @SpringBootTest):
- `backend/src/test/java/com/northstar/funding/integration/DiscoveryWorkflowIntegrationTest.java`
- `backend/src/test/java/com/northstar/funding/integration/DuplicateDetectionIntegrationTest.java`

**Copy the exact pattern from these files for your test type.**

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

## User Feedback (Verbatim)

> "Is that an abstract ancestor? What about all the repository interfaces are they being tested? don't they get their own tests"

This decision was made after user clarified that each repository should have its own dedicated test class, not a single monolithic integration test.

## Related Documentation

- [[001-text-array-over-jsonb]] - Database design patterns
- `CLAUDE.md` - TestContainers Best Practices section
- `backend/src/test/java/com/northstar/funding/integration/` - Reference implementations
