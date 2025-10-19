# NorthStar Funding Discovery - Project Context

**This file provides persistent context for Claude Code across all sessions.**

## Project Overview

**NorthStar Funding Intelligence Platform** - Automated funding discovery workflow with human-AI collaboration for discovering, validating, and managing funding opportunities (grants, scholarships, fellowships, awards, etc.).

## Constitution & Governance

- **Primary Authority**: `.specify/memory/constitution.md` (v1.1.0)
- **Spec-Kit Driven**: All features follow `/specify` → `/plan` → `/tasks` → `/implement` workflow
- **Key Principles**:
  - XML Tag Accuracy (CRITICAL) - Never use `<n>` tags, always full names
  - Domain-Driven Design - "Funding Sources" not "Grants"
  - Human-AI Collaboration - Every automated process includes human validation
  - Contact Intelligence Priority - Contact data is the highest value asset

## Infrastructure Setup (CRITICAL)

### Mac Studio (192.168.1.10)
**All infrastructure runs in Docker containers**:
- PostgreSQL 16: `northstar-postgres` container, port 5432
- Qdrant: `qdrant` container, ports 6333/6334
- SearXNG: `searxng` container, port 8080
- pgAdmin: `northstar-pgadmin` container, port 5050
- LM Studio: Runs NATIVELY on Mac Studio (NOT in Docker)

**Docker Compose**: `docker/docker-compose.yml`

### MacBook M2 (Development Machine)
**Code development ONLY**:
- NO psql binary installed
- NO local database servers
- NO production services
- Maven builds and tests using TestContainers

### Database Setup (IMPORTANT)

**Single Production Database**:
- Database: `northstar_funding` (ONLY ONE - no dev/test variants)
- User: `northstar_user`
- Password: `northstar_password`
- Location: Mac Studio Docker container `northstar-postgres`
- Schema: Managed by Flyway migrations
- Initial Types: `docker/config/init-db.sql` (PostgreSQL TYPE definitions)

**Database Access**:

**From Mac Studio**:
```bash
docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding
```

**From MacBook M2**:
- Spring Boot app connects to 192.168.1.10:5432 (requires Docker running on Mac Studio)
- Tests use TestContainers (ephemeral PostgreSQL containers, NOT the Mac Studio database)
- NO psql binary available (use SSH to Mac Studio for direct access)

**Flyway Migrations**:
```bash
# Run from MacBook M2 (connects to Mac Studio database)
mvn flyway:migrate
mvn flyway:info

# IMPORTANT: Early Development Phase (CURRENT WORKFLOW)
# When Domain model classes need changes:
# 1. Modify EXISTING migration files (V1-V7) to match new schema
# 2. Run: mvn flyway:clean  (wipes Mac Studio database)
# 3. Run: mvn flyway:migrate (recreates from modified migrations)
#
# DO NOT create V8, V9, etc. - modify existing files instead
# This workflow ends once we have production data worth keeping
```

**Docker Compose Workflow**:
1. Modify `docker/docker-compose.yml` on MacBook M2
2. Kevin rsyncs to Mac Studio
3. Kevin restarts containers on Mac Studio: `docker compose up -d`

## Technology Stack (NON-NEGOTIABLE)

- **Language**: Java 25 (Oracle JDK via SDKMAN)
- **Framework**: Spring Boot 3.5.6
- **Build**: Maven 3.9.9
- **Database**: PostgreSQL 16 (in Docker)
- **Vector DB**: Qdrant (for future RAG)
- **AI**: LM Studio locally hosted models
- **Testing**: JUnit 5 + TestContainers + REST Assured
- **Patterns**: Virtual Threads, Functional Programming (Vavr), DDD

## Project Structure

```
northstar-funding/
├── .claude/                     # Claude Code configuration
│   ├── commands/                # Slash commands (/specify, /plan, /tasks, etc.)
│   └── project-context.md       # This file (persistent memory)
├── .specify/                    # Spec-Kit framework
│   ├── memory/
│   │   └── constitution.md      # Project constitution (source of truth)
│   ├── scripts/                 # Workflow automation
│   └── templates/               # Spec/Plan/Tasks templates
├── backend/                     # Spring Boot backend (Java 25)
│   ├── src/main/java/           # Application code
│   ├── src/test/java/           # Tests (TestContainers)
│   ├── pom.xml                  # Maven configuration
│   └── README.md                # Backend-specific docs
├── docker/                      # Docker infrastructure
│   ├── docker-compose.yml       # All services definition
│   ├── .env                     # Environment configuration
│   └── README.md                # Infrastructure docs
├── specs/                       # Feature specifications
│   └── 001-automated-funding-discovery/
│       ├── spec.md              # Feature specification
│       ├── plan.md              # Implementation plan
│       ├── tasks.md             # Task list
│       ├── data-model.md        # Domain model design
│       └── contracts/           # API contracts
└── frontend/                    # Streamlit admin UI (future)
```

## Current Development Status

**Feature**: 001 - Automated Funding Discovery Workflow
- ✅ Specification complete
- ✅ Implementation plan generated
- ✅ Design artifacts created (data model, service layer, persistence)
- ✅ TDD strategy defined (TestContainers for integration tests)
- 📋 Tasks defined in `specs/001-automated-funding-discovery/tasks.md`
- 🚧 Implementation in progress

## Key Workflows

### Spec-Kit Commands
```bash
/specify [description]   # Create feature specification
/plan [details]          # Generate implementation plan
/tasks                   # Generate task list
/implement               # Execute implementation
/constitution            # Update constitution
```

### Development Workflow
```bash
# On MacBook M2
mvn clean compile        # Build
mvn test                 # Unit tests (TestContainers)
mvn integration-test     # Integration tests (connects to Mac Studio)
mvn spring-boot:run      # Run locally (connects to Mac Studio DB)
```

### Database Workflow
```bash
# Flyway migrations (from MacBook M2)
mvn flyway:migrate       # Run migrations on Mac Studio DB
mvn flyway:info          # Check migration status

# Direct DB access (SSH to Mac Studio first)
ssh 192.168.1.10
docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding
```

### Deployment Workflow (Kevin Manages)
```bash
# Kevin rsyncs to Mac Studio, then:
ssh 192.168.1.10
cd ~/northstar-infra
docker compose up -d
```

## Constitutional Constraints

### Never Do (Constitutional Violations)
- ❌ Use abbreviated XML tags (e.g., `<n>` instead of `<name>`)
- ❌ Create scripts without explicit permission (Principle IX)
- ❌ Execute rsync or deployment commands (Kevin manages, Principle VIII)
- ❌ Run production services on MacBook M2
- ❌ Create more than 4 core services (Complexity Management)
- ❌ Use "grants" terminology (use "funding sources")

### Always Do (Constitutional Requirements)
- ✅ Include human validation workflows for AI automation
- ✅ Treat contact information as first-class entities
- ✅ Follow DDD ubiquitous language ("Funding Sources")
- ✅ Use TestContainers for integration tests
- ✅ Write tests before implementation (TDD)
- ✅ Use Vavr for error handling and Optional types
- ✅ Enable Virtual Threads for I/O operations

### Development Phase Workflows

**Early Development (NOW) - Schema Iteration Workflow**:
1. Domain model class needs changes (e.g., add field to FundingSourceCandidate)
2. Modify corresponding migration file (e.g., V1__create_funding_source_candidate.sql)
3. Run `mvn flyway:clean` (wipes Mac Studio database completely)
4. Run `mvn flyway:migrate` (recreates from modified migrations)
5. Repeat as needed - NO V8, V9, etc. files created

**Later (Production Data Exists) - Standard Flyway Workflow**:
- Create new migration files (V8, V9, etc.)
- Never modify existing migrations (they're immutable)
- Preserve existing data through incremental changes

## Domain Model (Ubiquitous Language)

**Core Entities**:
- **Funding Source**: Grant, scholarship, fellowship, award, contract, sponsorship, loan, tax credit
- **Organization**: Foundation, government, NGO, religious institution, corporation
- **Contact Intelligence**: First-class entity with relationship tracking
- **Discovery Session**: Automated search execution record
- **Enhancement Record**: Audit trail of human improvements

**Application Lifecycle**:
Discovery → Research → Application → Outcome → Learning

## Common Issues & Solutions

### "psql: command not found"
**Solution**: There is NO psql binary on MacBook M2 or Mac Studio host.
- Use `docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding` on Mac Studio
- Application code connects to 192.168.1.10:5432 directly

### "Can't connect to PostgreSQL"
**Solution**: Verify Docker is running on Mac Studio:
```bash
ssh 192.168.1.10
docker ps | grep northstar-postgres
```

### "Flyway migration failed"
**Solution**: Check Mac Studio PostgreSQL is accessible:
```bash
mvn flyway:info  # Should show connection success
```

## Contact & Support

- **Project Owner**: Kevin (kevin@northstar.bg)
- **Team**: Kevin + Huw (admin users)
- **Deployment**: Mac Studio (192.168.1.10)
- **Development**: MacBook M2

## Memory Hierarchy

When in doubt, consult in this order:
1. `.specify/memory/constitution.md` - Source of truth for all decisions
2. `.claude/project-context.md` - This file (infrastructure & setup)
3. `backend/README.md` - Backend-specific instructions
4. `docker/README.md` - Infrastructure-specific instructions
5. `specs/*/spec.md` - Feature-specific requirements

---

**Last Updated**: 2025-10-18
**Constitution Version**: 1.1.0
**Status**: Active Development (Feature 001)
