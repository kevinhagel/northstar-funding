# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NorthStar Funding Discovery is a **planned** automated funding discovery platform. Currently, the project consists of a **foundational domain model and persistence layer** with **no application logic implemented yet**.

### Current State (What Actually Exists)

✅ **Domain Model**: 19 entities representing funding sources, organizations, programs, domains, sessions, etc.
✅ **Persistence Layer**: 9 Spring Data JDBC repositories + 5 service classes providing business logic
✅ **Database Schema**: 17 Flyway migrations creating complete database structure
✅ **Unit Tests**: 110 Mockito-based tests (all passing)
✅ **Multi-Module Maven Project**: 5 modules with clean separation of concerns

❌ **NO Crawler** - northstar-crawler module is empty
❌ **NO Search Integration** - No search engine adapters
❌ **NO AI Integration** - No LM Studio or AI-powered features
❌ **NO Application Layer** - No REST API, no orchestration, no scheduler
❌ **NO Judging** - northstar-judging module is empty

### Project Context

**Developer**: American expat living in Burgas, Bulgaria

This context influences:
- Geographic focus of funding searches (Eastern Europe, EU, Bulgaria-specific opportunities)
- Time zone considerations for development and scheduling
- Local infrastructure setup and connectivity patterns
- Target funding sources and regional priorities

## Technology Stack

### Core Technologies
- **Java 25** (source and target level 25) via SDKMAN
- **Spring Boot 3.5.6** with Spring Data JDBC (not JPA)
- **PostgreSQL 16** (Mac Studio @ 192.168.1.10:5432)
- **Vavr 0.10.7** for functional programming patterns (planned usage)
- **Lombok 1.18.42** for boilerplate reduction (domain entities only)

### Testing
- **JUnit 5** with Spring Boot Test
- **Mockito** for unit testing (110 tests implemented)
- **TestContainers** for PostgreSQL integration tests (planned, not yet implemented)

### Infrastructure
- **Flyway** for database migrations
- **PostgreSQL 16** running on Mac Studio @ 192.168.1.10:5432

## Build & Development Commands

### Maven Commands
```bash
# Build the project
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Compile only
mvn clean compile

# Test compile (useful for checking syntax)
mvn test-compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DomainServiceTest
mvn test -Dtest=OrganizationServiceTest

# Run all service tests
mvn test -Dtest='*ServiceTest'

# Install to local Maven repo
mvn clean install
```

### Database (Flyway)
```bash
# Apply all migrations
mvn flyway:migrate -pl northstar-persistence

# Clean database (DESTRUCTIVE - drops all objects)
mvn flyway:clean -pl northstar-persistence

# Clean and re-migrate (fresh start)
mvn flyway:clean flyway:migrate -pl northstar-persistence

# Show migration status
mvn flyway:info -pl northstar-persistence

# Validate migrations
mvn flyway:validate -pl northstar-persistence
```

**Note**: Flyway commands must use `-pl northstar-persistence` to target the correct module.

### Running the Application
**Currently there is NO application to run.** The northstar-application module is empty. Only the domain model and persistence layer exist.

## Architecture

### Multi-Module Maven Structure
```
northstar-funding/
├── pom.xml (parent POM)
├── northstar-domain/          # Domain entities
├── northstar-persistence/     # Repositories + Services + Flyway
├── northstar-crawler/         # (EMPTY - not implemented)
├── northstar-judging/         # (EMPTY - not implemented)
└── northstar-application/     # (EMPTY - not implemented)
```

### Domain Module (`northstar-domain`)

**19 Domain Entities** (all using Lombok):

**Core Workflow Entities:**
1. `FundingSourceCandidate` - Main workflow entity tracking funding source discovery
2. `Domain` - Domain-level deduplication and blacklist management
3. `Organization` - Funding organizations
4. `FundingProgram` - Specific funding programs offered by organizations
5. `SearchResult` - Search engine results (planned feature)

**Supporting Entities:**
6. `DiscoverySession` - Session tracking for discovery runs
7. `ContactIntelligence` - Contact information for organizations
8. `EnhancementRecord` - Tracking of data enrichment operations
9. `AdminUser` - System administrators

**Enums** (16 total):

**Core Workflow Enums (10):**
- `CandidateStatus` - NEW, PENDING_CRAWL, CRAWLED, ENHANCED, JUDGED, etc.
- `DomainStatus` - DISCOVERED, PROCESSED_HIGH_QUALITY, PROCESSED_LOW_QUALITY, BLACKLISTED, etc.
- `ProgramStatus` - ACTIVE, EXPIRED, SUSPENDED, etc.
- `SessionStatus`, `SessionType`, `ContactType`, `EnhancementType`
- `AdminRole`, `AuthorityLevel`, `SearchEngineType`

**Feature 005: Enhanced Taxonomy (6 new enums):**
- `FundingSourceType` - WHO provides funding (12 values: EU, Govt, NGO, Private, etc.)
- `FundingMechanism` - HOW funding distributed (8 values: Grant, Loan, Scholarship, etc.)
- `ProjectScale` - Amount ranges with BigDecimal (5 values: MICRO to MEGA)
- `BeneficiaryPopulation` - WHO benefits (18 values: age, demographics, needs)
- `RecipientOrganizationType` - WHAT TYPE receives (14 values: K12, University, NGO, etc.)
- `QueryLanguage` - ISO 639-1 codes (9 languages: BG, EN, DE, RO, FR, RU, EL, TR, SR)

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/`

### Persistence Module (`northstar-persistence`)

**9 Repository Interfaces** (Spring Data JDBC):
1. `FundingSourceCandidateRepository` - Main candidate management
2. `DomainRepository` - Domain deduplication and blacklist queries
3. `OrganizationRepository` - Organization lookup and validation
4. `FundingProgramRepository` - Program queries by status, deadline, etc.
5. `SearchResultRepository` - Search result queries (planned feature)
6. `DiscoverySessionRepository` - Session analytics and statistics
7. `ContactIntelligenceRepository` - Contact lookup
8. `EnhancementRecordRepository` - Enhancement tracking
9. `AdminUserRepository` - Admin user management

**5 Service Classes** (Business Logic):
1. `DomainService` - Domain registration, blacklist management, quality tracking
2. `OrganizationService` - Organization validation, confidence scoring
3. `FundingProgramService` - Program management, deadline tracking, expiration
4. `SearchResultService` - Search result processing, deduplication
5. `DiscoverySessionService` - Session statistics and analytics

**Service Layer Pattern:**
- Use `private final` fields for dependencies
- Explicit constructor (NO @Autowired, NO Lombok @RequiredArgsConstructor)
- Services are marked `@Service` and `@Transactional`
- Read-only methods use `@Transactional(readOnly = true)`

**Example Service Pattern:**
```java
@Service
@Transactional
public class DomainService {
    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    // Business methods...
}
```

**Location**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/`

### Database Schema (17 Flyway Migrations)

**Core Tables:**
- V1: `funding_source_candidate` - Main workflow table
- V2: `contact_intelligence` - Contact information
- V3: `admin_user` - System administrators
- V4: `discovery_session` - Session tracking
- V5: `enhancement_record` - Data enrichment tracking
- V6: Indexes for performance
- V7: Fix enhancement_record constraint
- V8: `domain` - Domain deduplication and blacklist
- V9: Update candidate_status enum for two-phase workflow

**Tables for Planned Features (Schema Exists, No Application Code):**
- V10: `search_queries` - Query library (planned)
- V11: `search_session_statistics` - Per-engine performance metrics (planned)
- V12: Extend discovery_session for search tracking
- V13: `query_generation_sessions` - AI query generation tracking (planned)
- V14: `metadata_judgments` - Phase 1 judging results (planned)
- V15: `organization` - Funding organizations
- V16: `funding_program` - Funding programs
- V17: `search_result` - Search engine results (planned)

**Location**: `northstar-persistence/src/main/resources/db/migration/`

## Database

### Connection Details
- **Host**: 192.168.1.10:5432 (Mac Studio)
- **Database**: northstar_funding
- **User**: northstar_user
- **Password**: northstar_password (configured in pom.xml Flyway plugin)

### Flyway Configuration
Located in `northstar-persistence/pom.xml`:
```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <configuration>
        <url>jdbc:postgresql://192.168.1.10:5432/northstar_funding</url>
        <user>northstar_user</user>
        <password>northstar_password</password>
    </configuration>
</plugin>
```

## Spring Data JDBC Best Practices

### TEXT[] for Simple Arrays
**Decision**: Use TEXT[] instead of JSONB for tags and engines to avoid complexity in early development.

**Pattern**:
```java
// Entity field (in some planned features)
private Set<String> tags;  // Stored as TEXT[] in PostgreSQL

// Custom converter (example from planned features)
@Component
public class StringSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}
```

**See**: `northstar-notes/decisions/001-text-array-over-jsonb.md` for detailed rationale.

### Custom Converters
Register custom converters in `northstar-persistence/src/main/java/com/northstar/funding/persistence/config/JdbcConfiguration.java`

Update `@EnableJdbcRepositories` annotation when adding new repository packages.

### Lombok Usage

**DO use Lombok for:**
- Domain entities (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)

**DO NOT use Lombok for:**
- Service classes (use explicit constructors for Spring DI)
- Test classes (explicit constructors are clearer)

**Lombok Configuration**:
Both `northstar-domain/pom.xml` and `northstar-persistence/pom.xml` include:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Testing Best Practices

### Unit Tests (Mockito)

**Current Status**: 110 unit tests implemented for all 5 service classes, all passing.

**Pattern**:
```java
@ExtendWith(MockitoExtension.class)
class DomainServiceTest {
    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainService domainService;

    @Test
    void registerDomain_WhenNew_ShouldCreateDomain() {
        // Given
        when(domainRepository.findByDomainName("test.org"))
            .thenReturn(Optional.empty());
        when(domainRepository.save(any(Domain.class)))
            .thenReturn(testDomain);

        // When
        Domain result = domainService.registerDomain("test.org", sessionId);

        // Then
        assertThat(result).isNotNull();
        verify(domainRepository).save(any(Domain.class));
    }
}
```

**Test Organization**:
- Unit tests: `northstar-persistence/src/test/java/com/northstar/funding/persistence/service/`
- 5 test classes: `DomainServiceTest`, `OrganizationServiceTest`, `FundingProgramServiceTest`, `SearchResultServiceTest`, `DiscoverySessionServiceTest`

### Integration Tests (TestContainers)

**Status**: NOT YET IMPLEMENTED. Planned for future development.

**When implemented**, follow this pattern:
```java
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("postgres-test")
public class YourIntegrationTest {
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
}
```

**See**: `northstar-notes/decisions/002-testcontainers-integration-test-pattern.md`

### Running Tests
```bash
# All tests
mvn test

# Specific service test
mvn test -Dtest=DomainServiceTest

# All service tests
mvn test -Dtest='*ServiceTest'
```

## Development Workflow

### Adding New Domain Entities
1. Create entity in `northstar-domain/src/main/java/com/northstar/funding/domain/`
2. Use Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
3. Use `@Table(name = "table_name")` for Spring Data JDBC
4. Create Flyway migration in `northstar-persistence/src/main/resources/db/migration/`
5. Create repository in `northstar-persistence/.../repository/`
6. Create service in `northstar-persistence/.../service/`
7. Write unit tests with Mockito

### Adding New Repositories
1. Create interface extending `CrudRepository<Entity, ID>`
2. Add custom query methods using `@Query` annotation
3. Update `JdbcConfiguration` if needed for custom converters
4. Write service layer methods that use the repository
5. Write unit tests for the service

### Adding New Services
1. Create service class with `@Service` and `@Transactional`
2. Use `private final` fields for repository dependencies
3. Create explicit constructor (NO @Autowired, NO Lombok)
4. Implement business logic methods
5. Use `@Transactional(readOnly = true)` for read-only methods
6. Write comprehensive Mockito unit tests

## Obsidian Vault Integration

The project includes an **Obsidian vault** at `northstar-notes/` for development notes, planning, and knowledge management.

### Vault Structure

```
northstar-notes/
├── .obsidian/           # Obsidian configuration (do not modify)
├── README.md            # Vault overview and usage guide
├── session-summaries/   # Claude Code session summaries
├── decisions/           # Architecture Decision Records (ADRs)
├── inbox/               # Quick capture, unprocessed notes
├── project/             # (empty - planned for project documentation)
├── architecture/        # (empty - planned for architecture docs)
├── technology/          # (empty - planned for tech notes)
├── features/            # (empty - planned for feature docs)
├── feature-planning/    # (empty - planned for WIP planning)
└── daily-notes/         # (empty - planned for daily logs)
```

### When Claude Code Should Write to the Vault

**MANDATORY - Always write to vault for:**

1. **Session Summaries** (`session-summaries/`)
   - After completing major feature work or milestone
   - After making architectural decisions
   - Format: `YYYY-MM-DD-description.md`

2. **Architecture Decision Records** (`decisions/`)
   - When making significant technical decisions
   - Format: `NNN-decision-title.md` (numbered sequentially)
   - Current ADRs:
     - 001-text-array-over-jsonb.md
     - 002-domain-level-deduplication.md
     - 002-testcontainers-integration-test-pattern.md

### Writing Conventions for Java Projects

**1. Link to Code Files:**
```markdown
See `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/DomainService.java:42`
```

**2. Link to Database Schema:**
```markdown
Migration `V8__create_domain.sql` adds domain deduplication
```

**3. Reference Java Classes:**
```markdown
The `DomainService` uses `DomainRepository` for persistence
```

**4. Reference Test Classes:**
```markdown
Added unit test: `DomainServiceTest.registerDomain_WhenNew_ShouldCreateDomain()`
```

## Key Design Decisions

### Domain-Level Deduplication
Use `Domain` entity to track unique domains and prevent reprocessing. See ADR: `northstar-notes/decisions/002-domain-level-deduplication.md`

### TEXT[] Instead of JSONB
Use PostgreSQL TEXT[] arrays for simple string collections to avoid Spring Data JDBC complexity with JSONB. See ADR: `northstar-notes/decisions/001-text-array-over-jsonb.md`

### Service Layer Pattern
- `private final` fields for dependencies
- Explicit constructors (NO @Autowired, NO Lombok)
- @Transactional for write operations
- @Transactional(readOnly = true) for read operations

### No Lombok in Services
Spring manages service beans, so we use explicit constructors for clarity and to avoid Lombok annotation processing issues with Spring DI.

## Project Structure

### Documentation
- `CLAUDE.md`: This file
- `specs/`: Feature specifications
  - `001-automated-funding-discovery/`: Original project spec (may be outdated)
- `northstar-notes/`: Obsidian vault for development notes
- `docs/`: (various design documents - may be outdated)

### External Infrastructure
All infrastructure runs on Mac Studio @ 192.168.1.10:
- PostgreSQL 16 (port 5432)

### Database Tables Created (But Application Code Not Implemented)

Several database tables exist for **planned features** that are **not yet implemented**:
- `search_queries` - Query library (V10)
- `search_session_statistics` - Per-engine metrics (V11)
- `query_generation_sessions` - AI query generation (V13)
- `metadata_judgments` - Phase 1 judging (V14)
- `search_result` - Search results (V17)

These tables were created in anticipation of future features but no application code uses them yet.

## What's Next?

The current implementation provides a solid foundation of domain entities and persistence layer. Future development might include:

1. **Application Layer** - REST API, orchestration, scheduling
2. **Crawler Infrastructure** - Web scraping and content extraction
3. **Search Integration** - Multiple search engine adapters
4. **AI Integration** - LM Studio for query generation and judging
5. **Judging Logic** - Metadata-based confidence scoring
6. **Integration Tests** - TestContainers-based integration testing

**However**, no specific features are planned or in progress. The roadmap is undefined.

## Troubleshooting

### Compilation Issues
- Ensure Lombok annotation processing is configured in both domain and persistence POMs
- Run `mvn clean compile` to regenerate Lombok-generated code
- Check that Java 25 is active: `java --version`

### Test Failures
- Ensure PostgreSQL is running at 192.168.1.10:5432
- Run Flyway migrations: `mvn flyway:migrate -pl northstar-persistence`
- Check test output for specific assertion failures

### Flyway Issues
- Always use `-pl northstar-persistence` with Flyway commands
- To reset database: `mvn flyway:clean flyway:migrate -pl northstar-persistence`
- Check migration status: `mvn flyway:info -pl northstar-persistence`

## Common Patterns

### Functional Error Handling (Vavr)
**Status**: Vavr is in the POM but not currently used. Planned for future application layer.

### BigDecimal for Confidence Scores (CRITICAL RULE)

**MANDATORY**: All confidence scores MUST use `BigDecimal` with scale 2 (two decimal places) for precision.

**Why**: Floating-point arithmetic causes precision errors. A confidence of 0.6 stored as `double` might become 0.5999999999999999, causing it to incorrectly fail our ≥ 0.6 threshold filter.

**Rules**:
1. **Always use `BigDecimal`** for confidence scores, never `double` or `float`
2. **Always set scale to 2**: `new BigDecimal("0.85").setScale(2, RoundingMode.HALF_UP)`
3. **Use String constructor**: `new BigDecimal("0.85")` not `new BigDecimal(0.85)`
4. **Database column**: `NUMERIC(3,2)` in PostgreSQL (values 0.00 to 1.00)
5. **Comparisons**: Use `.compareTo()` not `==` or `>=`

**Example**:
```java
// Creating confidence scores
BigDecimal confidence = new BigDecimal("0.85").setScale(2, RoundingMode.HALF_UP);
organization.setOrganizationConfidence(confidence);

// Comparing confidence scores (threshold filter)
BigDecimal threshold = new BigDecimal("0.60");
if (confidence.compareTo(threshold) >= 0) {
    // Pass: confidence >= 0.60
}
```

**Database Migration Pattern**:
```sql
ALTER TABLE funding_source_candidate
  ALTER COLUMN confidence_score TYPE NUMERIC(3,2);
```

## Important Notes

- **NO application layer exists** - only domain model and persistence
- **NO crawler, search, or judging features implemented**
- Database schema includes tables for planned features that don't exist yet
- All tests are unit tests with Mockito - no integration tests yet
- Service layer follows strict DI pattern (explicit constructors, no Lombok)
