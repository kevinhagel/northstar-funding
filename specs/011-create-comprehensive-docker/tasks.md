# Tasks: Docker-Based Integration Tests for REST API

**Feature**: 011-create-comprehensive-docker
**Branch**: `011-create-comprehensive-docker`
**Input**: Design documents from `/specs/011-create-comprehensive-docker/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

---

## Overview

This task list implements Docker-based integration tests for the REST API layer using TestContainers. The primary goal is to validate the existing Feature 009 implementation (REST → Query Generation → Kafka → Database workflow) without modifying application code unless tests reveal actual bugs.

**Key Constraints**:
- No application code changes unless tests find bugs
- Focus on test infrastructure and patterns
- Fix 5 existing failing repository tests
- Establish reusable patterns for future testing

**Execution Strategy**:
- Sequential: Infrastructure setup → Test utilities → Tests (parallel) → Documentation
- [P] = Parallel execution (independent files, no dependencies)
- Performance target: <5 minutes full test suite

---

## Phase A: Infrastructure Setup

### T001: Create AbstractIntegrationTest base class
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/AbstractIntegrationTest.java`

Create base class for all integration tests with:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@Testcontainers` and `@ActiveProfiles("integration-test")`
- Static PostgreSQL container: `postgres:16-alpine` with reuse enabled
- Static Kafka container: `confluentinc/cp-kafka:latest` with reuse enabled
- `@DynamicPropertySource` for dynamic property configuration
- `@BeforeAll` container verification method

**Acceptance**:
- Base class compiles successfully
- Containers configured with reuse=true
- Dynamic properties registered for datasource and Kafka

---

### T002: Create application-integration-test.yml configuration
**File**: `northstar-rest-api/src/test/resources/application-integration-test.yml`

Create Spring Boot test profile with:
- DataSource properties (will be overridden by @DynamicPropertySource)
- Kafka consumer/producer configuration (JSON serialization)
- Flyway enabled with migration locations
- Query generation configuration (Ollama settings)
- Debug logging for com.northstar.funding

**Acceptance**:
- YAML file is valid Spring Boot configuration
- Profile named "integration-test"
- Compatible with TestContainers property overrides

---

### T003: Create ~/.testcontainers.properties for remote Docker
**File**: `~/.testcontainers.properties` (on MacBook M2)

Create TestContainers configuration:
```properties
docker.host=tcp://192.168.1.10:2375
testcontainers.reuse.enable=true
```

**Acceptance**:
- File exists in home directory
- Contains docker.host and reuse settings
- Verify connectivity: `curl http://192.168.1.10:2375/version`

**Note**: This is a one-time setup task. Document in quickstart.md if not already covered.

---

### T004: Verify TestContainers connectivity
**Command**: Test that AbstractIntegrationTest can start containers

Create minimal test class:
```java
public class ContainerConnectivityTest extends AbstractIntegrationTest {
    @Test
    void containers_ShouldStartSuccessfully() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();
    }
}
```

Run: `mvn test -Dtest=ContainerConnectivityTest`

**Acceptance**:
- Test passes
- PostgreSQL container starts (<15 seconds)
- Kafka container starts (<15 seconds)
- Total startup <30 seconds

---

## Phase B: Test Utilities (Parallel)

### T005 [P]: Create TestFixtures utility class
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/util/TestFixtures.java`

Create test fixture methods:
- `validSearchRequest()`: Standard valid request with EU/National funding, grants/scholarships, Bulgaria scope
- `invalidSearchRequest_MissingFundingTypes()`: Empty fundingSourceTypes (validation error)
- `invalidSearchRequest_LowMaxResults()`: maxResultsPerQuery = 5 (below min 10)

**Acceptance**:
- Class compiles with all fixture methods
- Methods return SearchExecutionRequest instances
- Invalid fixtures trigger validation errors when used

---

### T006 [P]: Create ExpectedDatabaseState assertion utility
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/util/ExpectedDatabaseState.java`

Create database assertion methods:
- `assertSessionCreated(UUID sessionId, JdbcTemplate)`: Verify session exists with RUNNING status
- `assertNoSessionCreated(JdbcTemplate)`: Verify no sessions in database
- `assertSessionStatus(UUID sessionId, SessionStatus, JdbcTemplate)`: Verify specific status

**Acceptance**:
- Class compiles with all assertion methods
- Methods use AssertJ for fluent assertions
- Clear error messages on assertion failures

---

### T007 [P]: Create ExpectedKafkaEvents assertion utility
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/util/ExpectedKafkaEvents.java`

Create Kafka assertion methods:
- `assertEventsPublished(List<SearchRequestEvent>, UUID sessionId, int expectedCount)`: Verify event count and sessionId match
- `assertNoEventsPublished(List<SearchRequestEvent>)`: Verify no events
- `consumeKafkaEvents(UUID sessionId, Consumer)`: Helper to consume and filter events by sessionId

**Acceptance**:
- Class compiles with all assertion methods
- Methods use AssertJ for fluent assertions
- Event filtering by sessionId works correctly

---

## Phase C: Integration Tests (Parallel)

### T008 [P]: Implement SearchWorkflowIntegrationTest - Scenario 1 (Successful Flow)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/SearchWorkflowIntegrationTest.java`
**Contract**: `contracts/scenario-1-successful-flow.md`

Test: `executeSearch_WithValidRequest_CompletesFullWorkflow()`

GIVEN:
- Valid SearchExecutionRequest (via TestFixtures)
- Mock QueryGenerationService (returns 3 queries per engine)
- Mock DiscoverySessionService (returns session with UUID)

WHEN:
- POST /api/search/execute via TestRestTemplate

THEN:
- HTTP 200 OK
- Response sessionId not null (UUID format)
- Response queriesGenerated = 9 (3 engines × 3 queries)
- Database has 1 session (status=RUNNING, type=MANUAL)
- Kafka has 9 SearchRequestEvent messages with correct sessionId

**Acceptance**:
- Test compiles and extends AbstractIntegrationTest
- Mocks configured using @MockBean
- All assertions pass
- Test execution <10 seconds

---

### T009 [P]: Implement SearchWorkflowIntegrationTest - Scenario 2 (Invalid Request)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/SearchWorkflowIntegrationTest.java`
**Contract**: `contracts/scenario-2-invalid-request.md`

Test: `executeSearch_WithInvalidRequest_ReturnsValidationError()`

GIVEN:
- Invalid SearchExecutionRequest (empty fundingSourceTypes)

WHEN:
- POST /api/search/execute

THEN:
- HTTP 400 Bad Request
- Response contains validation error message
- Database has 0 session records
- Kafka has 0 events

**Acceptance**:
- Test compiles (same file as T008)
- All assertions pass
- No side effects from invalid request

---

### T010 [P]: Implement SearchWorkflowIntegrationTest - Scenario 5 (Concurrent Requests)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/SearchWorkflowIntegrationTest.java`

Test: `executeSearch_WithConcurrentRequests_HandlesAllSuccessfully()`

GIVEN:
- 5 valid SearchExecutionRequest instances

WHEN:
- Execute all 5 in parallel (CompletableFuture)

THEN:
- All 5 return 200 OK
- 5 unique sessionIds
- Database has 5 session records
- Kafka has 45 messages (5 × 9)

**Acceptance**:
- Test compiles (same file as T008, T009)
- Concurrent execution completes successfully
- All assertions pass
- No race conditions or data corruption

---

### T011 [P]: Implement DatabasePersistenceIntegrationTest - Scenario 3 (Database Verification)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/DatabasePersistenceIntegrationTest.java`
**Contract**: `contracts/scenario-3-database-verification.md`

Test: `sessionCreation_AfterSuccessfulRequest_PersistsCorrectly()`

GIVEN:
- Search request successfully processed

WHEN:
- Query database for session

THEN:
- Session exists with correct sessionId
- Session status = RUNNING
- Session type = MANUAL
- created_at within last 5 seconds
- updated_at not null

**Acceptance**:
- Test compiles and extends AbstractIntegrationTest
- Uses JdbcTemplate for direct database queries
- All assertions pass

---

### T012 [P]: Implement DatabasePersistenceIntegrationTest - No Session on Error
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/DatabasePersistenceIntegrationTest.java`

Test: `sessionCreation_AfterInvalidRequest_DoesNotPersist()`

GIVEN:
- Invalid search request (validation error)

WHEN:
- POST /api/search/execute returns 400

THEN:
- Database has 0 session records
- No orphaned data

**Acceptance**:
- Test compiles (same file as T011)
- Database remains clean after error
- Rollback works correctly

---

### T013 [P]: Implement KafkaIntegrationTest - Event Publication
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/KafkaIntegrationTest.java`

Test: `eventPublication_AfterSuccessfulRequest_PublishesCorrectEvents()`

GIVEN:
- Valid search request

WHEN:
- POST /api/search/execute completes

THEN:
- Kafka consumer receives 9 events
- All events have correct sessionId
- All events have non-blank query
- All events have maxResults >= 10

**Acceptance**:
- Test compiles and extends AbstractIntegrationTest
- Kafka consumer configured correctly
- All events consumed within 5 seconds
- All assertions pass

---

### T014 [P]: Implement KafkaIntegrationTest - Event Structure
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/integration/KafkaIntegrationTest.java`

Test: `eventStructure_PublishedEvents_ContainsRequiredFields()`

GIVEN:
- Valid search request processed

WHEN:
- Consume SearchRequestEvent from Kafka

THEN:
- Event has sessionId (UUID, not null)
- Event has query (String, not blank)
- Event has maxResults (Integer, >= 10)
- Event has timestamp (Instant, recent)

**Acceptance**:
- Test compiles (same file as T013)
- Event deserialization works correctly
- All field validations pass

---

## Phase D: Repository Test Fixes (Parallel)

### T015 [P]: Fix DomainRepositoryIntegrationTest
**File**: `northstar-persistence/src/test/java/com/northstar/funding/persistence/repository/DomainRepositoryIntegrationTest.java`

Rename from `DomainRepositoryTest` and update:
- Extend AbstractIntegrationTest (may need to move base class to shared location)
- Add `@Testcontainers` and `@ActiveProfiles("integration-test")`
- Remove any manual PostgreSQL container setup
- Verify all existing tests pass

**Acceptance**:
- Test renamed to *IntegrationTest
- Extends AbstractIntegrationTest
- All tests pass with TestContainers
- `mvn test -Dtest=DomainRepositoryIntegrationTest` succeeds

---

### T016 [P]: Fix FundingProgramRepositoryIntegrationTest
**File**: `northstar-persistence/src/test/java/com/northstar/funding/persistence/repository/FundingProgramRepositoryIntegrationTest.java`

Rename from `FundingProgramRepositoryTest` and update:
- Extend AbstractIntegrationTest
- Add required annotations
- Remove manual setup
- Verify tests pass

**Acceptance**:
- Same criteria as T015
- `mvn test -Dtest=FundingProgramRepositoryIntegrationTest` succeeds

---

### T017 [P]: Fix OrganizationRepositoryIntegrationTest
**File**: `northstar-persistence/src/test/java/com/northstar/funding/persistence/repository/OrganizationRepositoryIntegrationTest.java`

Rename from `OrganizationRepositoryTest` and update:
- Extend AbstractIntegrationTest
- Add required annotations
- Remove manual setup
- Verify tests pass

**Acceptance**:
- Same criteria as T015
- `mvn test -Dtest=OrganizationRepositoryIntegrationTest` succeeds

---

### T018 [P]: Fix AdminUserRepositoryIntegrationTest
**File**: `northstar-persistence/src/test/java/com/northstar/funding/persistence/repository/AdminUserRepositoryIntegrationTest.java`

Rename from `AdminUserRepositoryTest` and update:
- Extend AbstractIntegrationTest
- Add required annotations
- Remove manual setup
- Verify tests pass

**Acceptance**:
- Same criteria as T015
- `mvn test -Dtest=AdminUserRepositoryIntegrationTest` succeeds

---

### T019 [P]: Fix SearchResultRepositoryIntegrationTest
**File**: `northstar-persistence/src/test/java/com/northstar/funding/persistence/repository/SearchResultRepositoryIntegrationTest.java`

Rename from `SearchResultRepositoryTest` and update:
- Extend AbstractIntegrationTest
- Add required annotations
- Remove manual setup
- Verify tests pass

**Acceptance**:
- Same criteria as T015
- `mvn test -Dtest=SearchResultRepositoryIntegrationTest` succeeds

---

## Phase E: Documentation & Verification

### T020: Create DOCKER-SETUP.md documentation
**File**: `/Users/kevin/github/northstar-funding/DOCKER-SETUP.md`

Create comprehensive Docker setup guide:
1. **Overview**: Why Docker needed for integration tests
2. **Option 1: Remote Docker** (Mac Studio @ 192.168.1.10:2375)
   - Mac Studio configuration steps (for Kevin)
   - MacBook M2 ~/.testcontainers.properties setup
   - Verification commands
3. **Option 2: Local Docker Desktop**
   - Installation instructions
   - Verification steps
4. **Troubleshooting**: Common issues (connectivity, container startup failures, resource limits)
5. **Running Tests**: Maven commands from quickstart.md

**Acceptance**:
- Document created with clear step-by-step instructions
- Both remote and local Docker options documented
- Troubleshooting section covers common errors
- Links to quickstart.md for test execution

**Note**: Only create if Kevin approves script/documentation creation (Principle IX).

---

### T021: Update CLAUDE.md with testing patterns
**File**: `/Users/kevin/github/northstar-funding/CLAUDE.md`

Update testing section with:
- **Integration Test Patterns**: When to use AbstractIntegrationTest
- **TestContainers Configuration**: Remote Docker setup
- **Test Naming Convention**: *Test.java vs *IntegrationTest.java
- **Running Tests**: Maven commands for unit vs integration tests
- **Performance Expectations**: <5 minutes full suite

**Acceptance**:
- CLAUDE.md updated with testing section
- Patterns clearly documented
- Examples provided for common scenarios
- Consistent with existing CLAUDE.md style

---

### T022: Verify all integration tests pass with performance target
**Command**: Run full integration test suite

Execute:
```bash
# All integration tests
mvn test -Dtest='*IntegrationTest'

# Full suite (unit + integration)
mvn verify
```

**Acceptance Criteria**:
- All integration tests pass (0 failures, 0 errors)
- REST API integration tests: 7 tests passing
  - SearchWorkflowIntegrationTest: 3 tests
  - DatabasePersistenceIntegrationTest: 2 tests
  - KafkaIntegrationTest: 2 tests
- Repository integration tests: 5 tests passing
- Total execution time: <5 minutes (with container startup)
- Container reuse working (subsequent runs <3 minutes)

**Success Metrics**:
```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## Dependencies

**Phase Dependencies**:
- Phase B depends on Phase A complete (utilities need base class)
- Phase C depends on Phase B complete (tests use utilities)
- Phase D depends on Phase A complete (repository tests extend base class)
- Phase E depends on all tests passing (Phases C + D)

**Sequential Tasks**:
- T001 → T002 → T003 → T004 (infrastructure setup)
- T020 → T021 → T022 (documentation after tests working)

**Parallel Tasks**:
- T005, T006, T007 (independent utility classes)
- T008, T009, T010 (same file, but can be written in any order)
- T011, T012 (same file, but can be written in any order)
- T013, T014 (same file, but can be written in any order)
- T015, T016, T017, T018, T019 (independent repository test fixes)

---

## Parallel Execution Example

**Phase B - Launch T005, T006, T007 together:**
```
Task: "Create TestFixtures utility class"
Task: "Create ExpectedDatabaseState assertion utility"
Task: "Create ExpectedKafkaEvents assertion utility"
```

**Phase C - Launch T008, T011, T013 together (different files):**
```
Task: "Implement SearchWorkflowIntegrationTest - Scenario 1"
Task: "Implement DatabasePersistenceIntegrationTest - Scenario 3"
Task: "Implement KafkaIntegrationTest - Event Publication"
```

**Phase D - Launch T015, T016, T017, T018, T019 together:**
```
Task: "Fix DomainRepositoryIntegrationTest"
Task: "Fix FundingProgramRepositoryIntegrationTest"
Task: "Fix OrganizationRepositoryIntegrationTest"
Task: "Fix AdminUserRepositoryIntegrationTest"
Task: "Fix SearchResultRepositoryIntegrationTest"
```

---

## Validation Checklist

Before marking Feature 011 complete, verify:
- [ ] All contracts have corresponding tests (Scenarios 1-5 implemented)
- [ ] All test utilities created (TestFixtures, ExpectedDatabaseState, ExpectedKafkaEvents)
- [ ] All repository tests fixed (5 tests now passing)
- [ ] AbstractIntegrationTest shared across modules
- [ ] All tests extend base class correctly
- [ ] Performance target met (<5 minutes full suite)
- [ ] Documentation complete (DOCKER-SETUP.md if approved, CLAUDE.md updated)
- [ ] No application code changes (unless bugs found)

---

## Notes

- **TDD Approach**: Tests come before implementation, but Feature 009 already implemented. Tests validate existing code.
- **No Application Changes**: Unless tests reveal actual bugs (e.g., sessionId generation issue from Feature 009)
- **Commit After Each Task**: Each completed task should be a separate commit
- **Verify Tests Fail First**: If fixing repository tests, ensure they fail before applying fix
- **Container Reuse**: First run slow (image pull + startup), subsequent runs fast (<3 min)
- **Mock Ollama**: QueryGenerationService mocked to avoid external dependency on Mac Studio

---

**Total Tasks**: 22 (4 sequential setup + 3 parallel utilities + 7 parallel integration tests + 5 parallel repository fixes + 3 sequential documentation)

**Estimated Execution Time**:
- Phase A (setup): 30-45 minutes
- Phase B (utilities): 20-30 minutes (parallel)
- Phase C (integration tests): 2-3 hours (parallel)
- Phase D (repository fixes): 1-2 hours (parallel)
- Phase E (documentation): 30-45 minutes

**Total**: 4.5-6.5 hours development time
