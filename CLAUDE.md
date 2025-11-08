# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NorthStar Funding Discovery is a **planned** automated funding discovery platform. Currently, the project consists of a **foundational domain model and persistence layer** with **no application logic implemented yet**.

### Current State (What Actually Exists)

✅ **Domain Model**: 19 entities representing funding sources, organizations, programs, domains, sessions, etc.
✅ **Persistence Layer**: 9 Spring Data JDBC repositories + 5 service classes providing business logic
✅ **Database Schema**: 17 Flyway migrations creating complete database structure
✅ **Unit Tests**: 327 Mockito-based tests (all passing)
✅ **Multi-Module Maven Project**: 5 modules with clean separation of concerns
✅ **Search Result Processing**: SearchResultProcessor with confidence scoring, domain deduplication, and blacklist filtering
✅ **Query Generation Module**: AI-powered search query generation using Ollama (llama3.1:8b) with LangChain4j, 24-hour caching, and concurrent generation (58 tests passing)

⚠️ **Partial Crawler Implementation**: Search result processing pipeline (no web crawling yet)
❌ **NO Search Integration** - No search engine adapters (queries generated but not executed)
❌ **NO Application Layer** - No REST API, no orchestration, no scheduler
❌ **NO Judging Module** - northstar-judging module is empty

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
- **Spring Boot 3.5.7** with Spring Data JDBC (not JPA)
- **PostgreSQL 16** (Mac Studio @ 192.168.1.10:5432)
- **PostgreSQL JDBC Driver 42.7.8**
- **LangChain4j 1.8.0** for LLM integration (Ollama)
- **Vavr 0.10.7** for functional programming patterns (planned usage)
- **Lombok 1.18.42** for boilerplate reduction (domain entities only)

### Testing
- **JUnit 5** with Spring Boot Test
- **Mockito** for unit testing (259 tests implemented)
- **TestContainers 1.21.3** for PostgreSQL integration tests

### Infrastructure
- **Flyway 11.15.0** for database migrations
- **PostgreSQL 16** running on Mac Studio @ 192.168.1.10:5432
- **Ollama** (native on Mac Studio, NOT in Docker) for LLM inference with concurrent requests
- **Docker Compose** infrastructure on Mac Studio @ 192.168.1.10

### Ollama Configuration (Mac Studio)

**Installation**: Native installation (NOT Docker) to access Metal GPU acceleration

**Network Configuration**:
- Listening on `0.0.0.0:11434` (all network interfaces)
- Accessible from MacBook M2, Docker containers, and Perplexica
- Configured via `~/Library/LaunchAgents/homebrew.mxcl.ollama.plist` with explicit EnvironmentVariables section

**Models** (stored on T7 SSD at `/Volumes/T7-NorthStar/ollama-models`):
- `llama3.1:8b` - Primary model for query generation (reliable structured output)
- `qwen2.5:0.5b` - Fast but unreliable for structured output (not recommended)
- `phi3:medium` - Alternative model
- `nomic-embed-text` - Embedding model for Perplexica

**Environment Variables** (via launchd plist):
```bash
OLLAMA_HOST=0.0.0.0:11434                          # Listen on all interfaces
OLLAMA_MODELS=/Volumes/T7-NorthStar/ollama-models # Model storage on T7 SSD
OLLAMA_NUM_PARALLEL=10                             # Concurrent requests (key advantage over LM Studio)
OLLAMA_MAX_LOADED_MODELS=2                         # Max models in memory
OLLAMA_FLASH_ATTENTION=1                           # Enable Flash Attention optimization
OLLAMA_KV_CACHE_TYPE=q8_0                          # Quantized KV cache for memory efficiency
```

**Verifying Ollama**:
```bash
# From MacBook M2 or Docker container
curl http://192.168.1.10:11434/v1/models

# Check listening interface on Mac Studio
ssh macstudio "lsof -i :11434"
# Should show: *:11434 (all interfaces), not localhost:11434
```

**Why Native Installation**:
- Metal GPU acceleration requires native Ollama, not Docker
- Concurrent request support via `OLLAMA_NUM_PARALLEL=10` (LM Studio doesn't support this)
- Lower latency for query generation (multiple parallel requests)

### Development Machine Architecture

**MacBook M2** (Development Machine):
- IDE and code editing
- Git repository at `/Users/kevin/github/northstar-funding`
- Docker configuration files in `docker/` subdirectory

**Mac Studio** (Infrastructure Server @ 192.168.1.10):
- NO IDEs or development tools
- NOT used as an interactive user machine
- Runs infrastructure services only (PostgreSQL, Qdrant, SearXNG, Perplexica, Ollama)
- Docker files deployed to `~/northstar/` directory

**Critical Workflow:**
```bash
# After editing docker-compose.yml or config files in docker/ on MacBook M2:
rsync -av docker/ macstudio:~/northstar/
```

**Why This Matters:**
- All Docker infrastructure changes are made on MacBook M2 in the git repo
- Changes are synced to Mac Studio for deployment
- Mac Studio is treated as a headless server, not a development machine
- This keeps the git repository as the single source of truth

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

### Ollama Configuration (Mac Studio)

**Ollama runs natively on Mac Studio** (NOT in Docker) to leverage Metal GPU acceleration.

**Models Available:**
- `qwen2.5:0.5b` - 397 MB - Fast, lightweight model for simple tasks
- `llama3.1:8b` - 4.9 GB - General purpose chat and reasoning
- `phi3:medium` - 7.9 GB - Microsoft's efficient 14B parameter model
- `nomic-embed-text:latest` - 274 MB - Text embeddings for RAG/vector search

**Model Storage:** `/Volumes/T7-NorthStar/ollama-models/` (external SSD, ~13GB)

**Concurrent Request Configuration:**
- `OLLAMA_NUM_PARALLEL=10` - Maximum 10 concurrent inference requests
- `OLLAMA_MAX_LOADED_MODELS=2` - Keep 2 models in memory simultaneously
- `OLLAMA_FLASH_ATTENTION=1` - Enable flash attention optimization for Apple Silicon

**API Access:**
- From Mac Studio host: `http://localhost:11434`
- From Docker containers: `http://host.docker.internal:11434`
- Server binds to: `0.0.0.0:11434` (accessible from network)

**Verification:**
```bash
# List models
ssh macstudio "ollama list"

# Test API
curl http://192.168.1.10:11434/api/tags

# Test from Docker container
docker run --rm --network northstar-network curlimages/curl:latest \
  curl -s http://host.docker.internal:11434/api/tags
```

### Perplexica with Ollama (AI-Powered Search)

**Status:** ✅ Working with workaround

**Configuration Location:** `docker/docker-compose.yml` and `docker/perplexica/config.toml`

**Critical Environment Variables (docker-compose.yml):**
```yaml
environment:
  - SEARXNG_API_URL=http://searxng:8080
  - OLLAMA_BASE_URL=http://192.168.1.10:11434
  - OLLAMA_API_URL=http://192.168.1.10:11434
  - MODEL=llama3.1:8b
  - CHAT_MODEL=llama3.1:8b
  - ACTION_MODEL=llama3.1:8b
  - WRITER_MODEL=llama3.1:8b
  - CODER_MODEL=llama3.1:8b
  - EMBEDDING_MODEL=nomic-embed-text
```

**Known Issue - UI Auto-Discovery Bug:**
- Perplexica's UI auto-discovers all Ollama models and miscategorizes them
- UI shows all models in both "Chat Models" and "Embedding Models" sections
- **DO NOT use the UI to configure models** - it doesn't persist correctly

**Workaround:**
- Environment variables override UI configuration at runtime
- Ignore the incorrect UI model display
- The application works correctly despite UI showing wrong config
- Models are properly used: `llama3.1:8b` for chat, `nomic-embed-text` for embeddings

**Performance Notes:**
- First query is slow (~60+ seconds) while Ollama loads model into memory
- Subsequent queries should be faster (model stays loaded)
- Concurrent requests supported via Ollama (10 parallel)

**Access:**
- Web UI: http://192.168.1.10:3001
- Uses SearXNG (port 8080) for web search
- Uses Ollama (port 11434) for AI inference

### Running the Application
**Currently there is NO application to run.** The northstar-application module is empty. Only the domain model and persistence layer exist.

## Architecture

### Multi-Module Maven Structure
```
northstar-funding/
├── pom.xml (parent POM)
├── northstar-domain/          # Domain entities
├── northstar-persistence/     # Repositories + Services + Flyway
├── northstar-crawler/         # Search result processing (partial implementation)
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

### Query Generation Module (`northstar-query-generation`)

**Status**: ✅ **FULLY IMPLEMENTED** - AI-powered search query generation with concurrent execution

**Purpose**: Generate optimized search queries for funding discovery across multiple search engines using Ollama LLM.

**Key Features**:
- **AI-Powered Query Generation**: Uses Ollama (llama3.1:8b) via LangChain4j for intelligent query creation
- **Engine-Specific Strategies**: Different query styles for keyword engines (Brave, Serper, SearXNG) vs AI engines (Tavily)
- **24-Hour Caching**: Identical requests within 24 hours return cached results (Caffeine cache)
- **Concurrent Execution**: Virtual Threads enable parallel query generation for multiple search engines
- **Async Processing**: All operations non-blocking with CompletableFuture

**Components**:

**1. Query Generation Strategies** (`northstar-query-generation/src/main/java/...strategy/`):
- `KeywordQueryStrategy` - Short, focused queries (3-8 words) for Brave, Serper, SearXNG
  - Example: "Bulgaria educational funding grants", "Eastern Europe teacher scholarships"
- `TavilyQueryStrategy` - Long, contextual queries (12-40 words) for Tavily AI search
  - Example: "What European Union funding opportunities are available to support modernizing science and technology education programs in rural areas of Poland and Hungary?"
- **Preamble Filtering**: Both strategies filter out LLM preambles like "Here are N queries:"

**2. Services**:
- `QueryGenerationService` - Main orchestrator with concurrent generation support
  - `generateQueries()` - Single engine query generation
  - `generateForMultipleProviders()` - Parallel generation for 4+ engines using Virtual Threads
- `QueryCacheServiceImpl` - 24-hour Caffeine cache with database persistence

**3. Configuration** (`OllamaConfig.java`):
```java
@Bean
public ChatModel chatModel() {
    return OpenAiChatModel.builder()
        .baseUrl("http://192.168.1.10:11434/v1")  // Ollama OpenAI-compatible endpoint
        .apiKey("not-needed")                      // Ollama doesn't require API key
        .modelName("llama3.1:8b")                  // 8B model for reliable structured output
        .timeout(Duration.ofSeconds(60))           // 60s for larger model
        .maxTokens(150)
        .temperature(0.7)
        .build();
}
```

**4. Application Configuration** (`application.yml`):
```yaml
query-generation:
  lm-studio:
    base-url: http://192.168.1.10:11434/v1
    api-key: not-needed
    timeout-seconds: 60
    model-name: llama3.1:8b  # Reliable 8B model (qwen2.5:0.5b too small for structured output)
  cache:
    ttl-hours: 24
    max-size: 1000
```

**5. Query Parsing** (Critical Implementation Detail):
Both strategies parse LLM responses with robust filtering:
```java
private List<String> parseQueries(String response, int maxQueries) {
    return Arrays.stream(response.split("\n"))
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .filter(line -> !isPreamble(line))          // Filter preambles
        .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))  // Remove "1. " prefix
        .map(line -> line.replaceAll("^\"+|\"+$", ""))       // Remove quotes
        .filter(line -> !line.isEmpty())
        .limit(maxQueries)
        .collect(Collectors.toList());
}

private boolean isPreamble(String line) {
    String lower = line.toLowerCase();
    return lower.startsWith("here are") ||
           lower.startsWith("here is") ||
           lower.contains("search queries:") ||
           lower.contains("queries:");
}
```

**Integration Tests** (58 tests, all passing):
- `SingleProviderQueryGenerationTest` - Validates query generation for each engine
- `CacheHitTest` - Validates 24-hour cache behavior and fast retrieval (<50ms)
- `KeywordVsAiOptimizedTest` - Validates different query styles for keyword vs AI engines
- `MultiProviderParallelTest` - Validates concurrent generation for 4 engines simultaneously
- `QueryPersistenceTest` - Validates database persistence with correct metadata

**Test Coverage**:
- ✅ All 58 tests passing (100% pass rate)
- ✅ Concurrent generation completes in <30s for 4 engines (not sequential 4×5s=20s)
- ✅ Keyword queries: 3-10 words, focused and targeted
- ✅ AI queries: 12-40 words, contextual and natural language
- ✅ Cache hit retrieval: <50ms (vs seconds for LLM call)
- ✅ Database persistence: Saves query text, model name, engine, categories, geographic scope

**Model Selection** (llama3.1:8b chosen over qwen2.5:0.5b):
- ✅ **llama3.1:8b**: Reliably generates exact query counts, follows structured prompts, produces quality output
- ❌ **qwen2.5:0.5b**: Fast (397MB) but unreliable - sometimes generates 2 queries instead of 3, concatenates queries, includes numbering prefixes

**Key Design Decisions**:
- **Ollama over LM Studio**: Concurrent request support (`OLLAMA_NUM_PARALLEL=10`) enables parallel query generation
- **LangChain4j**: Provides OpenAI-compatible client with proper streaming, timeouts, and error handling
- **Virtual Threads**: Java 21+ feature enables efficient concurrent operations without complex thread pools
- **Caffeine Cache**: In-memory cache with TTL for fast retrieval of identical requests
- **Database Persistence**: Queries saved with metadata for analytics and audit trail

**Location**: `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/`

**Not Yet Implemented**:
- Integration with search engine adapters (queries generated but not executed)
- Dynamic prompt tuning based on search result quality
- Multi-language query generation (currently English only)

### Crawler Module (`northstar-crawler`)

**Status**: Partial implementation - search result processing pipeline only

**Implemented Components:**

**1. Processing Pipeline:**
- `SearchResultProcessor` - Main orchestrator for two-phase judging workflow
  - Domain extraction and deduplication (using HashSet)
  - Blacklist filtering (checks DomainService)
  - Confidence scoring (0.00 to 1.00 scale)
  - Threshold filtering (≥ 0.60 creates candidates)
  - Statistics tracking (ProcessingStatistics model)

**2. Scoring Services:**
- `ConfidenceScorer` - Multi-signal confidence calculation
  - TLD credibility scoring (via DomainCredibilityService)
  - Funding keyword detection (grants, scholarships, etc.)
  - Geographic relevance (Bulgaria, EU, Eastern Europe)
  - Organization type detection (Ministry, Commission, Foundation, University)
  - Compound boost for multiple signals (≥ 3 signals)
- `DomainCredibilityService` - TLD-based credibility scoring
  - Government TLDs: +0.20 (.gov, .mil, country codes)
  - Education TLDs: +0.15 (.edu, .ac.*)
  - Organization TLDs: +0.10 (.org, .int, .eu)
  - Commercial TLDs: +0.00 (.com, .net, .biz)
  - Spam TLDs: -0.30 (.xyz, .info, .top, etc.)
- `CandidateCreationService` - Creates FundingSourceCandidate entities
  - High confidence (≥ 0.60) → PENDING_CRAWL status
  - Low confidence (< 0.60) → SKIPPED_LOW_CONFIDENCE status

**3. Models:**
- `ProcessingStatistics` - Tracks processing metrics
  - Total results processed
  - Duplicates skipped (domain-level deduplication)
  - Blacklisted domains skipped
  - High confidence candidates created
  - Low confidence results (not created as candidates)
- `SearchResult` - Represents search engine result
  - URL, title, description (metadata only)
  - No web crawling performed at this stage

**Processing Pipeline Order (Critical):**
```
SearchResult → Extract Domain → Check Duplicate → Check Blacklist
→ Calculate Confidence → Filter by Threshold (≥ 0.60)
→ Register Domain → Create Candidate
```

**Key Design Decisions:**
- **Two-Phase Workflow**: Phase 1 (metadata judging) creates PENDING_CRAWL candidates for Phase 2 (deep crawling - not yet implemented)
- **Confidence Threshold**: 0.60 minimum to create candidates (saves processing resources)
- **BigDecimal Precision**: All confidence scores use BigDecimal (scale 2) to avoid floating-point precision errors
- **Domain Deduplication**: In-memory HashSet prevents duplicate domain processing within a session
- **Blacklist Before Scoring**: Check blacklist before calculating confidence to avoid unnecessary processing

**Unit Tests**: 7 comprehensive tests in SearchResultProcessorTest
- Empty results handling
- Duplicate domain detection
- Low confidence filtering
- High confidence candidate creation
- Blacklist enforcement
- Statistics tracking verification
- End-to-end realistic scenario (Horizon Europe, Fulbright, etc.)

**Location**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/`

**Not Yet Implemented:**
- Search engine adapters (Searxng, Tavily, Browserbase, Perplexity)
- Web crawling infrastructure
- Deep content extraction
- Parallel processing with Virtual Threads

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

**Query Generation Tables (Implemented):**
- V10: `search_queries` - Query library (used by northstar-query-generation module)

**Tables for Planned Features (Schema Exists, No Application Code):**
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

**Current Status**: 327 unit tests implemented across persistence layer (5 service classes) and crawler module, all passing.

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
- Persistence tests: `northstar-persistence/src/test/java/com/northstar/funding/persistence/service/`
  - 5 test classes: `DomainServiceTest`, `OrganizationServiceTest`, `FundingProgramServiceTest`, `SearchResultServiceTest`, `DiscoverySessionServiceTest`
- Crawler tests: `northstar-crawler/src/test/java/com/northstar/funding/crawler/`
  - `SearchResultProcessorTest` - 7 comprehensive tests covering all pipeline scenarios
  - `ConfidenceScorer`, `DomainCredibilityService`, `CandidateCreationService` tests

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

# Crawler module tests only
mvn test -pl northstar-crawler

# SearchResultProcessor tests
mvn test -Dtest=SearchResultProcessorTest
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

- **NO application layer exists** - only domain model, persistence, and search result processing
- **Partial crawler implementation** - search result processing pipeline only (no web crawling yet)
- **NO search engine adapters** - SearchResultProcessor exists but no search engines connected
- **NO judging module** - northstar-judging module is empty
- Database schema includes tables for planned features that don't exist yet
- All tests are unit tests with Mockito - no integration tests yet
- Service layer follows strict DI pattern (explicit constructors, no Lombok)
- All 327 tests passing (as of Story 1.3 completion)
