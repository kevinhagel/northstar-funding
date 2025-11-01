# NorthStar Funding Discovery - Obsidian Vault

**Project**: Automated Funding Discovery System
**Purpose**: AI-powered funding discovery for The NorthStar Foundation Bulgaria
**Location**: Burgas, Bulgaria
**Developer**: American expat developing tools to support educational funding discovery

---

## 🎯 Vision & Mission

**NorthStar Funding Discovery** is a **subscription-based SaaS platform** for automated funding discovery, serving NGOs, educational institutions, and social impact organizations across Eastern Europe and beyond.

### The Genesis
**2023-2024**: British Centre Burgas school needed funding → Early "Spring Crawler" internal tool
**2024**: Insight from fundsforngos.org and candid.org → Build better solution for paying clients
**2025**: NorthStar Foundation Bulgaria school proposal validates market need

### The Core Technical Challenge
We could discover **hundreds or thousands** of funding candidate websites, but **design disparity made scraping impossible**:
- Organizations structured sites differently
- Contact intelligence (emails, phones, names) scattered across pages
- Programs nested within organizations unpredictably
- No standard schema or markup

**Pure automation failed.** Too many edge cases, too much variability.

### The Breakthrough: Human-AI Hybrid Model
- **AI discovers and filters** (handles volume, speed, initial judgment)
- **Humans extract and enrich** (handles variability, contact intelligence, final judgment)
- **Together**: Scalable + High quality

## 📂 Vault Structure

```
northstar-notes/
├── README.md                    # This file - vault guide and index
├── project/                     # Project documentation
│   ├── vision-and-mission.md
│   ├── project-overview.md
│   └── development-roadmap.md
├── architecture/                # System architecture
│   ├── architecture-overview.md
│   ├── domain-model.md
│   ├── search-infrastructure.md
│   ├── rag-system.md
│   └── data-flow.md
├── technology/                  # Technology references
│   ├── tech-stack.md
│   ├── java-25-virtual-threads.md
│   ├── spring-boot.md
│   ├── postgresql.md
│   ├── lm-studio.md
│   ├── qdrant.md
│   └── resilience4j.md
├── features/                    # Feature documentation
│   ├── feature-001-foundation.md
│   ├── feature-002-automated-crawler.md
│   ├── feature-003-search-infrastructure.md
│   └── feature-004-metadata-judging.md
├── decisions/                   # Architecture Decision Records (ADRs)
│   ├── 001-text-array-over-jsonb.md
│   ├── 002-domain-level-deduplication.md
│   ├── 003-circuit-breaker-per-engine.md
│   └── 004-virtual-threads-parallel-search.md
├── session-summaries/           # Claude Code session summaries
│   ├── 2025-10-30-search-adapter-status.md
│   └── 2025-10-30-archived-search-patterns-analysis.md
├── daily-notes/                 # Daily work logs
│   └── YYYY-MM-DD.md
├── feature-planning/            # WIP feature planning
│   └── [drafts before formal specs]
└── inbox/                       # Quick capture, unprocessed
    └── personal-context.md
```

### 📅 daily-notes/
Daily work logs, progress tracking, and quick notes from development sessions.

**Usage**: Create one note per day (e.g., `2025-10-30.md`)

### 🤖 session-summaries/
Claude Code session summaries - what was accomplished, decisions made, blockers encountered.

**Usage**: Claude Code writes here after completing major work or feature milestones.

### 🎯 feature-planning/
Work-in-progress planning, brainstorming, and design thoughts before they become formal specs.

**Usage**: Draft ideas here, then move to `/specs` when solidified.

### 🏗️ architecture/
System architecture documentation, domain model, data flow diagrams.

**Usage**: Document high-level system design, component interactions, architectural patterns.

### 💻 technology/
Technology stack deep dives, configuration patterns, best practices.

**Usage**: Document specific technologies, setup guides, troubleshooting, patterns.

### ✨ features/
Feature-specific documentation linking to formal specs with implementation notes.

**Usage**: Living documentation for each major feature, progress tracking, lessons learned.

### 🔍 decisions/
Architecture Decision Records (ADRs) and important technical decisions.

**Current ADRs**:
- `002-domain-level-deduplication.md` - Domain-level deduplication strategy (IMPLEMENTED)
- `003-testcontainers-integration-test-pattern.md` - Integration testing pattern (PLANNED)
- `archived/001-text-array-over-jsonb.md` - TEXT[] usage (archived - not yet relevant)

**Usage**: Document "why" behind major choices. Mark status as Implemented/Proposed/Archived.

### 🗂️ project/
High-level project documentation, vision, mission, roadmap.

**Usage**: Project overview, business context, goals, timeline.

### 📥 inbox/
Quick capture for random thoughts, TODOs, links, and ideas that need processing later.

**Usage**: Brain dump here, then organize into proper folders.

---

## 🗺️ Navigation - Key Documents

### Start Here
- [[project-overview]] - Complete project overview and motivation
- [[vision-and-mission]] - The NorthStar Foundation Bulgaria vision
- [[architecture-overview]] - System architecture and design
- [[tech-stack]] - Technology stack and infrastructure

### Architecture & Design
**NOTE**: Architecture documentation not yet created. See:
- `CLAUDE.md` - Current project state (domain model + persistence layer only)
- `specs/` - Feature specifications with design details

### Features
**NOTE**: Feature documentation not yet created. Current features tracked in `/specs` directory:
- `specs/002-bigdecimal-confidence-scores/` - BigDecimal verification (COMPLETE)

### Decisions
- [[002-domain-level-deduplication]] - Domain-level deduplication (IMPLEMENTED)
- [[003-testcontainers-integration-test-pattern]] - TestContainers pattern (PLANNED)
- [[archived/001-text-array-over-jsonb]] - TEXT[] vs JSONB (ARCHIVED - not yet relevant)

### Technology Deep Dives
**NOTE**: Technology documentation not yet created. Current tech stack documented in `CLAUDE.md`:
- Java 25 with Virtual Threads
- Spring Boot 3.5.6 + Spring Data JDBC
- PostgreSQL 16 @ Mac Studio (192.168.1.10:5432)
- Flyway for migrations
- Lombok for domain entities
- JUnit 5 + Mockito for testing

---

## 🏷️ Common Tags

### Project Phase
- `#planning` - Planning and design phase
- `#implementation` - Active development
- `#testing` - Testing and validation
- `#deployed` - Deployed to production
- `#research` - Research and exploration

### Component
- `#search-infrastructure` - Search engine integration
- `#domain-model` - Entity design and database
- `#rag` - RAG system and vectorization
- `#crawler` - Web crawling and discovery
- `#ai-judging` - AI-powered judging and scoring

### Technology
- `#java` - Java 25 code and patterns
- `#spring-boot` - Spring Boot framework
- `#postgres` - PostgreSQL database
- `#kafka` - Event streaming (future)
- `#lm-studio` - Local LLM inference
- `#qdrant` - Vector database

### Feature Numbers
- `#feature-001` - Foundation
- `#feature-002` - Automated Crawler
- `#feature-003` - Search Infrastructure
- `#feature-004` - Metadata Judging

### Document Types
- `#architecture` - Architecture documentation
- `#adr` - Architecture Decision Record
- `#session-summary` - Development session summary
- `#bug` - Bug reports and fixes
- `#refactor` - Refactoring notes
- `#performance` - Performance optimization

---

## 🔗 External Links

### Project Resources
- GitHub Repository: `github.com/kevin/northstar-funding`
- [CLAUDE.md](../CLAUDE.md) - Claude Code project guide
- [Technical Docs](../docs/) - Detailed technical documentation
- [Feature Specs](../specs/) - Feature specifications and implementation plans

### Infrastructure
- PostgreSQL: Mac Studio @ 192.168.1.10:5432
- Searxng: Mac Studio @ http://192.168.1.10:8080
- LM Studio: Mac Studio @ http://192.168.1.10:1234/v1

### Technology Documentation
- [Java 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [Spring Boot 3.5.x](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JDBC](https://spring.io/projects/spring-data-jdbc)
- [PostgreSQL 16](https://www.postgresql.org/docs/16/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Qdrant](https://qdrant.tech/documentation/)
- [LM Studio](https://lmstudio.ai/)
- [Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)

---

## 📝 Documentation Conventions

### Linking to Code
```markdown
See `backend/src/main/java/com/northstar/funding/discovery/service/MetadataJudgingService.java:42`
```

### Linking to Database Schema
```markdown
Migration `V14__create_metadata_judgments_table.sql` adds confidence scoring
```

### Linking to Tests
```markdown
Integration test: `MondayNightlyDiscoveryTest.shouldLoadCorrectQueries()`
```

### Cross-References
```markdown
See [[architecture-overview]] for system design
Related decision: [[001-text-array-over-jsonb]]
Feature spec: [Feature 004](../specs/004-ai-query-generation-metadata-judging/spec.md)
```

### Tags
```markdown
#feature-003 #search-infrastructure #java #performance
```

---

## 🎓 Learning Resources

### Bulgaria & Eastern Europe Funding Context
- EU Horizon Programs
- EEA/Norway Grants
- America for Bulgaria Foundation
- Bulgaria poverty statistics (30% at risk, 21% below poverty line in Burgas)

### Educational Funding Types
- Capital Grants (buildings/infrastructure)
- Program Grants
- Scholarships
- Capacity Building
- Operational Support

---

## 🚀 Quick Start for New Sessions

1. **Check today's daily note**: `daily-notes/YYYY-MM-DD.md`
2. **Review recent sessions**: `session-summaries/` for context
3. **Check feature status**: `features/` for current work
4. **Review blockers**: `inbox/` for unresolved issues
5. **Check decisions**: `decisions/` for architectural context

---

## 🛠️ Maintenance

### Weekly Review
- Process notes in `inbox/`
- Update `decisions/` index if new ADRs added
- Archive or expand notes in `feature-planning/`
- Review `session-summaries/` for patterns

### Session End
- Write session summary to `session-summaries/`
- Document major decisions in `decisions/`
- Update feature documentation in `features/`
- Clean up `inbox/`

---

## Obsidian + Claude Code Workflow

1. **Start of day**: Create daily note, review yesterday's progress
2. **During coding**: Ask Claude Code to write session summaries to `session-summaries/`
3. **Planning features**: Draft in `feature-planning/`, move to `/specs` when ready
4. **Quick thoughts**: Dump in `inbox/`, process weekly
5. **Major decisions**: Document in `decisions/` with context and rationale

## Tips

- Use `[[Wiki Links]]` to connect related notes
- Tag notes with `#feature-003`, `#bug`, `#architecture` for easy filtering
- Link to specific code files: `backend/src/main/java/...`
- Claude Code can read from and write to this vault

---

**Last Updated**: 2025-11-01
**Vault Owner**: Kevin (American expat in Burgas, Bulgaria)
**Project Status**: Active Development - Domain Model + Persistence Layer Complete (163 tests passing)
