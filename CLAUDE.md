# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

NorthStar Funding Discovery is an automated funding discovery platform for EU/Eastern Europe opportunities. Currently implemented: foundational domain model, persistence layer, query generation module, and partial search result processing.

### Current State

✅ **Domain Model**: 19 entities + 16 enums (funding sources, organizations, programs, domains, sessions)
✅ **Persistence Layer**: 9 Spring Data JDBC repositories + 5 service classes
✅ **Database Schema**: 17 Flyway migrations
✅ **Query Generation Module**: AI-powered query generation (Ollama + LangChain4j, 24-hour caching, 58 tests)
✅ **Search Result Processing**: Confidence scoring, domain deduplication, blacklist filtering
✅ **Tests**: 327 unit tests (Mockito), integration tests (TestContainers)

⚠️ **Partial**: Crawler (processing pipeline only), REST API (needs admin endpoints)
❌ **Missing**: Search engine adapters, web crawling, judging module

### Project Context
Developer: American expat in Burgas, Bulgaria
- Geographic focus: Eastern Europe, EU, Bulgaria-specific funding
- Infrastructure: Mac Studio (192.168.1.10) + MacBook M2 (dev machine)

## Technology Stack

**Backend:**
- Java 25 + Spring Boot 3.5.7 + Spring Data JDBC (not JPA)
- PostgreSQL 16 (192.168.1.10:5432)
- LangChain4j 1.8.0 for Ollama integration
- Lombok 1.18.42 (domain entities only)
- Flyway 11.15.0, TestContainers 1.21.3

**Frontend (Admin Dashboard):**
- Vue 3.4+ TypeScript, Vite 5.0
- PrimeVue 3.50 (FREE), PrimeIcons 7.0
- Pinia 2.1, Axios 1.6, Chart.js 4.4

**Infrastructure (Mac Studio @ 192.168.1.10):**
- PostgreSQL 16 (port 5432)
- Ollama (native, NOT Docker - Metal GPU acceleration)
- Docker Compose (SearXNG, Perplexica, etc.)

### Ollama Configuration

**Native Installation** (NOT Docker) for Metal GPU acceleration

**Models** (on `/Volumes/T7-NorthStar/ollama-models`):
- `llama3.1:8b` - Primary for query generation (reliable structured output)
- `qwen2.5:0.5b` - Fast but unreliable for structured output
- `phi3:medium`, `nomic-embed-text`

**Key Environment Variables:**
```bash
OLLAMA_HOST=0.0.0.0:11434
OLLAMA_NUM_PARALLEL=10           # Concurrent requests (advantage over LM Studio)
OLLAMA_MAX_LOADED_MODELS=2
OLLAMA_FLASH_ATTENTION=1
```

**Access:**
- Mac Studio: `http://localhost:11434`
- Docker containers: `http://host.docker.internal:11434`
- Network: `http://192.168.1.10:11434`

**Verify:** `curl http://192.168.1.10:11434/v1/models`

### Development Machine Architecture

**MacBook M2** (Development):
- IDE, code editing
- Git repo: `/Users/kevin/github/northstar-funding`
- Docker config files in `docker/` subdirectory

**Mac Studio** (Headless Infrastructure):
- NO IDEs or interactive use
- Runs: PostgreSQL, Ollama, Docker services
- Files deployed to `~/northstar/`

**Critical Workflow:**
```bash
# After editing docker-compose.yml on MacBook M2:
rsync -av docker/ macstudio:~/northstar/
```

## Build & Development

### Maven
```bash
mvn clean package              # Build
mvn clean compile              # Compile only
mvn test                       # All tests
mvn test -Dtest=ServiceTest    # Specific test
```

### Flyway (Database)
```bash
# Always use -pl northstar-persistence
mvn flyway:migrate -pl northstar-persistence
mvn flyway:clean flyway:migrate -Dflyway.cleanDisabled=false -pl northstar-persistence  # Fresh start
mvn flyway:info -pl northstar-persistence
```

**Dev Note**: Safe to rewrite migrations in development, use clean + migrate to reset.

### Running Tests
```bash
# Unit tests (no Docker)
mvn test -Dtest='!*IntegrationTest'

# Integration tests (requires Docker)
mvn test -Dtest='*IntegrationTest'

# Specific module
mvn test -pl northstar-crawler
```

## Architecture

### Multi-Module Maven Structure
```
northstar-funding/
├── northstar-domain/           # 19 entities + 16 enums
├── northstar-persistence/      # 9 repositories + 5 services + Flyway
├── northstar-query-generation/ # AI query generation (Ollama)
├── northstar-crawler/          # Search result processing (partial)
├── northstar-rest-api/         # REST API (needs admin endpoints)
└── northstar-admin-ui/         # Vue admin dashboard
```

### Key Modules

**Domain** (`northstar-domain/`):
- Core: `FundingSourceCandidate`, `Domain`, `Organization`, `FundingProgram`
- Supporting: `DiscoverySession`, `ContactIntelligence`, `EnhancementRecord`
- Enums: `CandidateStatus`, `DomainStatus`, `FundingSourceType`, `QueryLanguage`, etc.

**Persistence** (`northstar-persistence/`):
- 9 repositories (Spring Data JDBC)
- 5 services: Domain, Organization, FundingProgram, SearchResult, DiscoverySession
- Service pattern: `private final` fields, explicit constructors (NO @Autowired, NO Lombok)

**Query Generation** (`northstar-query-generation/`):
- AI-powered query generation via Ollama (llama3.1:8b)
- Engine-specific strategies: Keyword (3-8 words) vs AI (12-40 words)
- 24-hour Caffeine cache, Virtual Threads for parallel generation
- 58 integration tests (100% passing)

**Crawler** (`northstar-crawler/`):
- `SearchResultProcessor` - Domain deduplication, blacklist, confidence scoring
- `ConfidenceScorer` - TLD credibility, keywords, geo-relevance
- Threshold: ≥0.60 → PENDING_CRAWL, <0.60 → SKIPPED_LOW_CONFIDENCE
- 7 unit tests

### Database

**Connection:** `postgresql://192.168.1.10:5432/northstar_funding` (user: northstar_user)

**17 Migrations:**
- V1-V9: Core tables (candidate, domain, session, contact, enhancement)
- V10: search_queries (used by query-generation)
- V11-V17: Planned features (organization, funding_program, search_result)

## Coding Standards

### Service Layer Pattern
```java
@Service
@Transactional
public class DomainService {
    private final DomainRepository domainRepository;

    // Explicit constructor (NO @Autowired, NO Lombok)
    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Domain> findByName(String name) { ... }
}
```

### Lombok Usage
- **DO**: Domain entities (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- **DON'T**: Services (explicit constructors), test classes

### BigDecimal for Confidence Scores (CRITICAL)

**MANDATORY**: All confidence scores use `BigDecimal` with scale 2.

```java
// Creating scores
BigDecimal confidence = new BigDecimal("0.85").setScale(2, RoundingMode.HALF_UP);

// Comparing (use .compareTo(), NOT ==)
if (confidence.compareTo(new BigDecimal("0.60")) >= 0) { ... }
```

**Why**: Floating-point causes precision errors (0.6 becomes 0.599999...).

**Database**: `NUMERIC(3,2)` for 0.00-1.00 range.

### Spring Data JDBC

**TEXT[] for Arrays**: Use PostgreSQL TEXT[] instead of JSONB for simple string collections.
See: `northstar-notes/decisions/001-text-array-over-jsonb.md`

**Custom Converters**: Register in `JdbcConfiguration.java`

## Admin Dashboard (Vue + PrimeVue)

**Status**: Architecture designed, Feature 013 ready for implementation

**Purpose**: Web UI for Kevin/Huw to review, enhance, approve funding candidates.

**Architecture**: Vue 3 (port 5173) → REST API (port 8080) → PostgreSQL (192.168.1.10:5432)

**Key Principle**: Domain entities stay in service layer. REST uses DTOs (String types only).

**Development**:
```bash
mvn spring-boot:run -pl northstar-rest-api        # Terminal 1
cd northstar-admin-ui && npm run dev              # Terminal 2 (Vite proxy handles CORS)
```

**Feature Roadmap**: Review Queue → Enhancement → Contact AI → Approval → Stats → Domains

**Full Details**: See `northstar-notes/architecture/admin-dashboard-architecture.md` for complete DTO patterns, UI mockups, API endpoints, and data flow diagrams.

## Development Workflows

### Adding Domain Entities
1. Create in `northstar-domain/.../domain/`
2. Use Lombok + `@Table(name="...")`
3. Create Flyway migration
4. Create repository + service
5. Write Mockito unit tests

### Adding Services
1. `@Service` + `@Transactional`
2. `private final` fields + explicit constructor
3. `@Transactional(readOnly = true)` for reads
4. Write comprehensive unit tests

## Obsidian Vault (`northstar-notes/`)

**Structure:**
- `session-summaries/` - Feature completion summaries
- `decisions/` - Architecture Decision Records (ADRs)
- `architecture/` - Architecture docs
- `inbox/` - Quick notes

**When to Write:**
- After completing major features
- When making architectural decisions
- Format: `YYYY-MM-DD-description.md` (summaries), `NNN-title.md` (ADRs)

**Link Conventions:**
```markdown
See `northstar-persistence/.../DomainService.java:42`
Migration `V8__create_domain.sql` adds...
The `DomainService` uses `DomainRepository`
```

## Key Design Decisions

1. **Domain-Level Deduplication**: Track unique domains to prevent reprocessing
   - See: `northstar-notes/decisions/002-domain-level-deduplication.md`

2. **TEXT[] Over JSONB**: Simpler Spring Data JDBC integration
   - See: `northstar-notes/decisions/001-text-array-over-jsonb.md`

3. **Two-Phase Workflow**: Phase 1 (metadata judging) → Phase 2 (deep crawling)

4. **Ollama Over LM Studio**: Concurrent request support (`OLLAMA_NUM_PARALLEL=10`)

## Troubleshooting

**Compilation**: `mvn clean compile` regenerates Lombok code

**Flyway**: Always use `-pl northstar-persistence`, reset with `flyway:clean flyway:migrate`

**PostgreSQL**: Verify running at 192.168.1.10:5432, run migrations

**Ollama**: Verify with `curl http://192.168.1.10:11434/v1/models`

## Important Notes

- Branch: `014-create-automated-crawler` (current work)
- Main branch: `main` (for PRs)
- Database: Rewriting migrations OK in dev (use flyway:clean)
- Mac Studio: Headless server (NO interactive use)
- Sync workflow: Edit on MacBook M2 → rsync to Mac Studio
- Tests: 327 unit tests passing, integration tests require Docker
