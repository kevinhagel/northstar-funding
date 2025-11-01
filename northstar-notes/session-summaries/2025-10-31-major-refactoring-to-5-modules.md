# Major Refactoring: Backend → 5-Module Maven Structure

**Date**: 2025-10-31
**Duration**: In Progress
**Trigger**: Kevin identified backend/ as "unfocused garbage and noise"
**Decision**: Complete restructure to proper Maven multi-module monolith

---

## Problem Statement

**Kevin's Assessment**:
> "The backend/ project is full of noise, it is very unfocused, it looks like several programmers have been working on it at the same time without communicating with each other. backend/ is NOT the search module, it is the persistence module, the Domain Driven Design Model module"

**Root Causes**:
1. Everything crammed into single `backend/` module
2. Search adapters mixed with domain entities
3. Feature 003 and 004 work was poorly planned
4. No clear separation of concerns
5. Monolithic Ball of Mud anti-pattern

---

## The Good Work (Preserved)

**What Kevin built that's excellent**:
- ✅ Domain entities (Candidate, Domain, ContactIntelligence, DiscoverySession)
- ✅ Flyway migrations (V1-V12, clean schema evolution)
- ✅ Spring Data JDBC repositories
- ✅ Excellent unit + integration tests with TestContainers
- ✅ Repository services
- ✅ DDD principles properly applied

**This is the foundation we're preserving and building on.**

---

## Architectural Decision: 5-Module Maven Monolith

**NOT Microservices** (learned lesson from springcrawler Kafka hell)

**Structure**:
```
northstar-funding/
├── pom.xml (parent)
├── northstar-domain/          # Pure domain (zero dependencies)
├── northstar-persistence/     # Repositories + Flyway
├── northstar-crawler/         # 4 search engines + dedup
├── northstar-judging/         # Metadata judging
└── northstar-application/     # Spring Boot main + scheduler
```

**Benefits**:
- Clean separation of concerns
- Independent module testing
- Proper dependency management (domain has zero deps)
- Can deploy as single JAR or split later
- No Kafka, no microservices complexity

---

## Domain Model Clarification

### The Hierarchy (from Kevin)

```
Organization (America for Bulgaria)
  ↓ owns
Domain (us-bulgaria.org)
  ↓ hosts
FundingProgram (Education Grant 2025)
  ↓ has
URL (us-bulgaria.org/education-grant)
  ↓ has
SearchResult (metadata from search engines)
```

### Two-Level Judging

1. **Domain-level**: Homepage → Organization metadata (once per domain)
2. **Program-level**: Each URL → Program metadata (multiple per domain)

### Deduplication Rules

- Same domain + same URL + same day → **Skip (duplicate)**
- Same domain + different URL → **Process (different program)**
- New domain → **Judge org + judge program**

---

## Search Infrastructure Corrected

**Kevin wants 4 search engines**:
1. **BraveSearch** - Keyword searches → metadata
2. **SearXNG** - Keyword searches → metadata
3. **Serper** (Google API) - Keyword searches → metadata
4. **Tavily** - **AI prompts** (not keywords) → metadata

**NOT Browserbase** (that's for crawling, not search - Feature 005+)

**Location**: `northstar-crawler/` module (NOT in persistence)

---

## Actions Taken So Far

### 1. Archived Current Work
```bash
cp -r backend/ archived-backend/backend-20251031/
```

**Preserved**: All existing code as reference

### 2. Created Parent POM

**File**: `/pom.xml`

**Key Properties**:
- Java 25
- Spring Boot 3.5.6
- Lombok 1.18.42
- Vavr 0.10.7 (functional programming)
- Resilience4j 2.2.0
- PostgreSQL 42.7.5
- Flyway 10.26.0
- TestContainers 1.20.4

### 3. Created northstar-domain/ Module

**File**: `/northstar-domain/pom.xml`

**Dependencies**: Lombok only (zero external dependencies)

**Purpose**: Pure domain entities

### 4. Created northstar-persistence/ Module

**File**: `/northstar-persistence/pom.xml`

**Dependencies**:
- northstar-domain
- Spring Data JDBC
- PostgreSQL
- Flyway
- TestContainers (test scope)

**Purpose**: Repositories, migrations, data access

---

## Domain Entities To Extract

### From Archived Backend

**Core Entities** (keep and refine):
- ✅ `FundingSourceCandidate` - Main aggregate root
- ✅ `Domain` - Deduplication tracking
- ✅ `ContactIntelligence` - Contact data (PII protection)
- ✅ `DiscoverySession` - Session tracking
- ✅ `EnhancementRecord` - Enhancement history
- ✅ `AdminUser` - User management

**Enums** (keep):
- `CandidateStatus`
- `DomainStatus`
- `ContactType`
- `AuthorityLevel`
- `SessionType`
- `SessionStatus`
- `EnhancementType`

### New Entities To Create

**Organization**:
```java
@Table("organization")
class Organization {
    UUID organizationId;
    String name;              // "America for Bulgaria Foundation"
    String domain;            // "us-bulgaria.org" (FK)
    String mission;
    String contactEmail;
    LocalDateTime discoveredAt;
}
```

**FundingProgram**:
```java
@Table("funding_program")
class FundingProgram {
    UUID programId;
    UUID organizationId;      // FK
    String programName;
    String url;               // Full program URL
    BigDecimal amountMin;
    BigDecimal amountMax;
    LocalDate deadline;
}
```

**SearchResult** (for deduplication):
```java
@Table("search_result")
class SearchResult {
    UUID resultId;
    String url;
    String domain;
    String title;
    String snippet;
    SearchEngineType source;  // Which engine found it
    LocalDate discoveredDate;
    Boolean judgedAsOrg;
    Boolean judgedAsProgram;
}
```

---

## Next Steps (In Progress)

### Immediate (Today)

1. ✅ Archive backend/
2. ✅ Create 5-module structure
3. ⏳ Extract domain entities to northstar-domain/
4. ⏳ Extract Flyway migrations to northstar-persistence/
5. ⏳ Extract repositories to northstar-persistence/
6. ⏳ Extract tests to northstar-persistence/

### Short-term (This Sprint)

7. ⏳ Create remaining modules (crawler, judging, application)
8. ⏳ Archive Feature 003/004 specs
9. ⏳ Create new Persistence Module spec
10. ⏳ Update all documentation

### Medium-term (Next Sprint)

11. Implement 4 search adapters in northstar-crawler/
12. Implement two-level judging in northstar-judging/
13. Implement scheduler in northstar-application/

---

## Specs and Documentation To Update

### Delete/Archive

- `specs/003-search-execution-infrastructure/` - Wrong approach
- `specs/004-ai-query-generation-metadata-judging/` - Not implemented yet

### Create

- `specs/001-persistence-module/` - Domain + Persistence foundation
- `specs/002-crawler-module/` - 4 search engines properly done
- `specs/003-judging-module/` - Two-level judging
- `specs/004-scheduler/` - Nightly discovery orchestration

### Update

- `CLAUDE.md` - New 5-module structure
- `README.md` - Updated architecture
- All Obsidian vault docs

---

## Key Architectural Principles

### 1. Pure Domain Layer
- `northstar-domain/` has ZERO dependencies (except Lombok)
- No Spring annotations in domain entities
- Can be used by any module

### 2. Persistence Isolation
- `northstar-persistence/` depends only on `northstar-domain`
- Contains all Flyway migrations
- All Spring Data JDBC repositories
- All integration tests

### 3. Crawler Independence
- `northstar-crawler/` handles search only
- 4 search engines, proper adapter pattern
- Depends on persistence for saving results

### 4. Judging Separation
- `northstar-judging/` handles metadata evaluation
- Two-level judging (organization + program)
- Depends on domain + persistence

### 5. Application Orchestration
- `northstar-application/` ties everything together
- Single executable JAR
- Scheduler lives here
- Can be split later if needed

---

## Lessons Learned

### What Went Wrong

1. **Feature 003 was unfocused** - Mixing search with persistence
2. **Browserbase confusion** - Not a search engine, it's for crawling
3. **No clear module boundaries** - Everything in one big backend/
4. **Poor planning** - Should have designed modules first

### What We're Fixing

1. **Clear module boundaries** - Each module has single responsibility
2. **Proper dependency management** - Domain is pure, no circular deps
3. **Better planning** - Specs match architecture
4. **Focus on persistence first** - Foundation before features

---

## Status

**Current**: Creating 5-module structure, extracting domain entities

**Next**: Extract migrations and repositories, complete persistence module

**Goal**: Clean, focused, maintainable codebase with proper separation of concerns

---

**End of Summary**
