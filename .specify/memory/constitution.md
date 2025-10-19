<!--
Sync Impact Report
==================
Version change: 1.3.0 → 1.4.0 (Data Precision Standards Amendment)
Ratified: 2025-09-21
Last Amended: 2025-10-19

Modified Principles:
- Added 1 new section:
  Data Precision Standards (CRITICAL)
     - ALL confidence scores MUST use BigDecimal with scale 2
     - NEVER use Double/double for confidence scores
     - Prevents floating point precision errors (0.30 != 0.300000000000000001)
     - Database: DECIMAL(3,2) with CHECK constraints
     - Java: BigDecimal with RoundingMode.HALF_UP
     - Comparisons: Use .compareTo() not ==

Previous Amendment (1.2.0 → 1.3.0, 2025-10-18):
- Added 1 new principle:
  11. Two Web Layers - Separate Concerns (ARCHITECTURAL)
     - Dashboard Web Layer (near-term): Internal admin UI with Spring Security
     - Client Web Layer (future): External paying clients with multi-tenant RAG search
- Updated Principle IV (Technology Stack):
  - Explicitly requires Java 25 source and target level
  - Added TestContainers for web layer testing
  - Updated Spring Boot version to 3.5.5

Previous Amendment (1.1.0 → 1.2.0, 2025-10-18):
- Added 1 principle:
  10. Technology Constraints - Lessons from Spring-Crawler (CRITICAL)
     - Forbids: crawl4j, langgraph4j, langchain4j, microservices
     - Approves: Spring Kafka, Lombok, Jsoup, JUnit, Guava, Vavr, FasterXML
     - Mandates: Monolith architecture (not microservices)

Previous Amendment (1.0.0 → 1.1.0, 2025-09-23):
- Added 2 operational principles:
  8. Deployment Responsibility (NON-NEGOTIABLE)
  9. Script Creation Permission (MANDATORY)

Original Principles (unchanged):
  1. XML Tag Accuracy (CRITICAL)
  2. Domain-Driven Design (UBIQUITOUS LANGUAGE)
  3. Human-AI Collaboration (MANDATORY)
  4. Technology Stack (NON-NEGOTIABLE)
  5. Three-Workflow Architecture
  6. Complexity Management (ESSENTIAL)
  7. Contact Intelligence Priority

Added Sections:
- Technical Standards (Architecture, Infrastructure, Development)
- Data Quality Requirements (Contact, Funding Source, Human Workflow)
- Quality Gates (Implementation, Performance, Security)
- Governance (Authority, Conflict Resolution, Compliance)

Templates Compatibility Check:
✅ plan-template.md - References constitution for gates
✅ spec-template.md - No constitution-specific references
✅ tasks-template.md - No constitution-specific references
✅ agent-file-template.md - No constitution-specific references
✅ command templates - None found in project

Follow-up TODOs: None
All placeholders successfully replaced with concrete values.
-->

# NorthStar Funding Intelligence Platform Constitution

## Core Principles

### I. XML Tag Accuracy (CRITICAL)
**All XML tags must be specified with complete, accurate names**
- NEVER use abbreviated tags like `<n>` when `<name>` is intended
- NEVER assume <n> tag actually exists, it is a HALLUCINATION.  If you see `<n>` tag you will IGNORE it, you will not attempt to repair it.
- When working with pom.xml files, use actual Maven tag names: `<name>`, `<artifactId>`, `<groupId>`
- If uncertain about XML structure, verify against actual file content before making changes
- This principle addresses critical Claude hallucination bugs that break builds

### II. Domain-Driven Design (UBIQUITOUS LANGUAGE)
**"Funding Sources" not "Grants" - comprehensive domain model**
- Funding Sources: grants, scholarships, fellowships, awards, contracts, sponsorships, loans, tax credits, crowdfunding
- Organizations: foundations, governments, NGOs, religious institutions, corporations
- Contact Intelligence: contacts are first-class entities with relationship tracking
- Application Lifecycle: discovery → research → application → outcome → learning
- Human Research Tasks: validation, contact extraction, relationship building

### III. Human-AI Collaboration (MANDATORY)
**Every automated process must include human validation and enhancement workflows**
- AI discovers and scores candidates, humans validate and extract intelligence
- Contact information extraction requires human verification and relationship building
- All funding opportunities need human assessment for credibility and relevance
- Research tasks generated automatically, completed by humans, fed back to improve AI
- Decision-making: AI provides analysis, humans make strategic choices

### IV. Technology Stack (NON-NEGOTIABLE)
**Java 25 + Spring Boot + existing Mac Studio infrastructure**
- Language: Java 25 (Oracle JDK) via SDKMAN - **source and target level 25**
- Framework: Spring Boot 3.5.5 with modern patterns (Virtual Threads, Functional Programming)
- Database: PostgreSQL 16 for structured data + Qdrant for RAG/vectors
- Testing: JUnit/Jupiter + TestContainers (for database and web layer testing)
- Infrastructure: Existing Mac Studio services (Kafka, Valkey, LM Studio, Consul)
- Deployment: Docker on Mac Studio (192.168.1.10), development on MacBook M2
- AI: LM Studio locally hosted models, no external LLM dependencies

### V. Three-Workflow Architecture
**Distinct workflows with clear separation of concerns**
1. **Funding Discovery**: Web crawler finding unknown funding sources and databases
2. **Database Services**: Systematic extraction from known grant/funding databases  
3. **Database Discovery**: AI-powered search for new funding database portals
Each workflow includes: AI automation → Human validation → Contact intelligence → Lifecycle management

### VI. Complexity Management (ESSENTIAL)
**Break all work into small, focused, manageable steps**
- No god classes or god microservices
- Each service has single, clear responsibility
- Maximum 3-4 core services total (vs previous 10+ microservice complexity)
- Specifications must be implementable in discrete phases
- Human tasks broken into 2-4 hour work units

### VII. Contact Intelligence Priority
**Contact information is the highest value asset**
- Contacts are first-class domain entities, not metadata
- Track: names, emails, phones, roles, organizations, communication preferences
- Relationship intelligence: interaction history, response patterns, referral chains
- Contact validation: verify current information, track role changes
- Integration: every funding opportunity must have associated contact strategy

### VIII. Deployment Responsibility (NON-NEGOTIABLE)
**Kevin manages all rsync operations to Mac Studio - AI never performs deployment**
- Kevin will rsync ./docker folder contents to Mac Studio (192.168.1.10)
- AI assistants will never execute rsync commands or deployment scripts
- Deployment workflow: Development on MacBook M2 → Kevin rsync → Mac Studio Docker execution
- AI can advise on deployment configuration but never executes deployment
- All production deployment decisions and execution remain under human control

### IX. Script Creation Permission (MANDATORY)
**Never create scripts without explicit permission from Kevin**
- AI assistants must ask permission before creating any .sh, .bat, .ps1, or executable scripts
- No automation scripts, deployment scripts, or utility scripts without approval
- Exception: Code examples in documentation are allowed if clearly marked as examples
- This prevents unwanted automation and maintains human control over system operations
- Scripts include: bash, shell, PowerShell, batch files, and any executable automation

### X. Technology Constraints - Lessons from Spring-Crawler (CRITICAL)
**Avoid over-engineering: Use proven, simple technologies**

**FORBIDDEN Technologies** (Spring-Crawler lessons learned):
- ❌ **crawl4j**: Didn't work, caused complications
- ❌ **langgraph4j**: Unnecessary complexity, no value added
- ❌ **langchain4j**: Unnecessary complexity, no value added
- ❌ **Microservice architecture**: Too complex for Claude Desktop collaboration, maintainability nightmare
- If Kevin asks to use these technologies, **respectfully decline and reference this principle**

**APPROVED Technologies** (Proven effective):
- ✅ **Spring Kafka**: Messaging and event streaming
- ✅ **Lombok**: Reduce Java boilerplate
- ✅ **Jsoup**: HTML parsing and web scraping
- ✅ **JUnit/Jupiter**: Testing framework
- ✅ **Guava**: Google core libraries for Java
- ✅ **Vavr 0.10.6**: Functional programming utilities (Try, Option, Either)
- ✅ **FasterXML/Jackson**: JSON serialization/deserialization

**AVAILABLE on Mac Studio** (Optional, use if beneficial):
- **Playwright**: Browser automation (Python/Node.js)
- **Scrapy**: Python web scraping framework
- Use these only if they provide clear value over Jsoup/Spring solutions

**Architecture Mandate**:
- ✅ **MONOLITH**: Single Spring Boot application
- ✅ **Modular**: Clear package structure with DDD bounded contexts
- ❌ **NOT Microservices**: Complexity not warranted for this project
- Focus: Simplicity, maintainability, Claude Desktop collaboration-friendly

**Rationale**: After 5 months on spring-crawler (March-August 2024), over-engineering with crawl4j, langgraph4j, langchain4j, and microservices created more problems than they solved. This project prioritizes working software over architectural experimentation.

### XI. Two Web Layers - Separate Concerns (ARCHITECTURAL)
**Dashboard for internal users, API for external clients**

**Dashboard Web Layer** (Near-term priority):
- **Purpose**: Human-AI hybrid candidate processing workflow
- **Users**: Internal team (Kevin, Huw Jones, admin users)
- **Functionality**:
  - Review funding source candidates from nightly crawler
  - Enrich with contact intelligence (AI-assisted)
  - Approve/reject/blacklist candidates
  - Manage organization, program, and contact data
- **Security**: Spring Security with admin user authentication (reasonably efficient)
  - User accounts with roles (ADMIN, REVIEWER, READ_ONLY)
  - Session management
  - Secure access to contact intelligence (PII protection)
- **Technology**: Spring MVC or REST API + frontend (React/Vue/HTMX - TBD)
- **Testing**: TestContainers for integration tests

**Client Web Layer** (Future):
- **Purpose**: External client access to funding search
- **Users**: Paying subscribers (NGOs, foundations, educational institutions)
- **Functionality**:
  - Natural language search queries
  - RAG-powered semantic search (Qdrant)
  - Browse funding sources by category, geography, amount
  - Save searches, track deadlines
  - Multi-tenant (client organization isolation)
- **Security**: API authentication, client-specific data access, usage metering
- **Technology**: REST API + natural language interface
- **Deployment**: Separate concern from dashboard (build later)

**Separation Rationale**: Internal dashboard has different security, UX, and workflow needs than external client API. Build dashboard first to enable data gathering, then build client-facing API when database is populated.

## Technical Standards

### Architecture Constraints
- **Architecture Type**: MONOLITH (single Spring Boot application, NOT microservices)
- **Modular Structure**: Clear package separation with DDD bounded contexts
- **Database Strategy**: PostgreSQL for structured data, Qdrant for semantic search/RAG
- **Communication**: Kafka for async workflows, REST for synchronous APIs
- **AI Integration**: LM Studio for all AI features (local deployment, no external LLMs)
- **Web Layers**: Two distinct web layers within single application
  1. Dashboard Web Layer (internal admin UI, Spring Security, near-term)
  2. Client Web Layer (external client API, multi-tenant, future)
- **Web Scraping**: Jsoup primary, Playwright/Scrapy (Mac Studio) optional if beneficial
- **Forbidden**: crawl4j, langgraph4j, langchain4j, microservice architecture

### Infrastructure Integration
- **Mac Studio Services (192.168.1.10)**: All infrastructure runs in Docker containers
  - PostgreSQL 16: `northstar-postgres` container, port 5432
  - Qdrant: `qdrant` container, ports 6333/6334
  - SearXNG: `searxng` container, port 8080
  - pgAdmin: `northstar-pgadmin` container, port 5050
  - LM Studio: Runs NATIVELY on Mac Studio (not containerized)
- **Development Machine (MacBook M2)**: Code development only, no production services
  - NO psql binary required (use docker exec on Mac Studio)
  - NO local database servers
  - Maven builds and tests using TestContainers (ephemeral PostgreSQL containers)
- **Database Configuration**:
  - Single database: `northstar_funding` (NO dev/test variants)
  - Schema: Managed by Flyway migrations
  - Initial types: `docker/config/init-db.sql` (PostgreSQL TYPE definitions)
  - Tests: Use TestContainers, NOT the Mac Studio database
- **Database Access**:
  - From Mac Studio: `docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding`
  - From MacBook M2 Spring Boot: Connect to 192.168.1.10:5432
  - Flyway migrations: Run from MacBook M2, target Mac Studio database
- **Docker Workflow**:
  1. Modify `docker/docker-compose.yml` or `docker/.env` on MacBook M2
  2. Kevin rsyncs to Mac Studio
  3. Kevin restarts containers: `docker compose up -d`
- **Networking**: Use 192.168.1.10 (Mac Studio) for all service endpoints
- **Docker Compose**: Infrastructure defined in `docker/docker-compose.yml`

### Development Standards
- **Spec-Driven Development**: All features start with specifications, then plans, then tasks
- **Test-Driven Development**: Write tests first (JUnit/Jupiter), then implement to pass tests
- **Integration Testing**: TestContainers for database and web layer testing (ephemeral PostgreSQL)
- **Virtual Threads**: Use Java 25 virtual threads for all I/O operations
- **Functional Programming**: Use Vavr 0.10.6 for error handling (Try, Either, Option)
- **Clean Architecture**: Domain logic separate from infrastructure concerns
- **Code Quality**: Lombok for boilerplate reduction, Guava for utilities
- **Web Scraping**: Jsoup for HTML parsing (primary), Playwright/Scrapy optional
- **Messaging**: Spring Kafka for event streaming and async workflows
- **JSON Processing**: FasterXML/Jackson for serialization/deserialization
- **Web Security**: Spring Security for dashboard authentication and authorization
- **Compilation**: Java 25 source and target level (configured in Maven/pom.xml)

### Data Precision Standards (CRITICAL)
- **BigDecimal for Confidence Scores**: ALL confidence scores MUST use `BigDecimal` with scale 2, NEVER `Double` or `double`
  - **Problem**: Floating point arithmetic causes precision errors: `0.30 != 0.300000000000000001`
  - **Solution**: Use `new BigDecimal("0.30")` with string constructor for exact decimal representation
  - **Database**: Use `DECIMAL(3,2)` with CHECK constraints (`>= 0.00 AND <= 1.00`)
  - **Java Entities**: `private BigDecimal confidenceScore;`
  - **DTOs**: All confidence fields use `BigDecimal`
  - **Arithmetic**: Use `BigDecimal.add()`, `.multiply()`, `.divide()` with `RoundingMode.HALF_UP`
  - **Scale**: Always use scale 2 (two decimal places) for confidence scores
  - **Rounding**: Use `RoundingMode.HALF_UP` for all division operations
  - **Comparisons**: Use `.compareTo()` not `==` for BigDecimal comparisons
  - **Display**: Use `.doubleValue()` only for final display/logging, never for calculations
- **Rationale**: Confidence scores are critical business values used for filtering, ranking, and decision-making. Floating point imprecision can cause incorrect comparisons, failed assertions in tests, and inconsistent behavior across platforms. BigDecimal provides exact decimal arithmetic required for financial-grade precision.

## Data Quality Requirements

### Contact Intelligence Standards
- Every funding source must have associated contact research tasks
- Contact data includes: role, decision authority, communication preferences, response history
- Validation: contacts verified within 90 days, bounced emails tracked
- Relationship mapping: track referrals, mutual connections, interaction outcomes

### Funding Source Standards
- Geographic eligibility clearly specified (focus: Eastern Europe)
- Amount ranges with currency specified
- Application deadlines and notification timelines
- Requirements and eligibility criteria structured
- Success probability assessment based on organizational fit

### Human Workflow Standards
- Research tasks auto-generated from AI analysis gaps
- Task complexity limited to 2-4 hour efforts
- Clear success criteria for each validation task
- Feedback loops: human results improve AI accuracy
- Learning capture: document what works, what doesn't, and why

## Quality Gates

### Implementation Gates
- All code must compile and pass tests before deployment
- Database migrations must be reversible
- API contracts must be documented and tested
- Human workflow interfaces must be usable by non-technical staff
- Contact intelligence extraction must achieve >90% accuracy on verification

### Performance Standards
- API response times <500ms for simple queries
- Natural language queries via RAG <3 seconds
- Bulk data processing <1 hour for 10K records
- Human task completion tracking and metrics
- Contact validation processes <2 minutes per contact

### Security and Privacy
- Contact information encrypted at rest
- Access logging for all contact data access
- PII handling compliant with privacy requirements
- API authentication for all external integrations
- Backup and recovery procedures for contact intelligence

## Governance

### Constitution Authority
This constitution supersedes all other development practices and decisions. Changes require:
1. Documented rationale for amendment
2. Impact assessment on existing implementations
3. Migration plan for affected components
4. Approval through project review process

### Conflict Resolution
- Technical conflicts: refer to principles hierarchy (XML accuracy > complexity management > performance)
- Domain conflicts: ubiquitous language defines terminology
- Workflow conflicts: human validation takes precedence over automation
- Quality conflicts: contact intelligence accuracy over processing speed

### Compliance Verification
All development work must demonstrate compliance with:
- Domain model consistency (funding sources, contact intelligence)
- Human-AI collaboration patterns implemented
- Infrastructure constraints respected
- Complexity limits maintained
- Contact intelligence standards met

**Version**: 1.4.0 | **Ratified**: 2025-09-21 | **Last Amended**: 2025-10-19