<!-- 
Sync Impact Report
==================
Version change: 1.0.0 → 1.1.0 (Deployment & Script Control Amendment)
Ratified: 2025-09-21
Last Amended: 2025-09-23

Modified Principles:
- Added 2 new operational principles:
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
- Language: Java 25 (Oracle JDK) via SDKMAN
- Framework: Spring Boot 3.x with modern patterns (Virtual Threads, Functional Programming)
- Database: PostgreSQL 16 for structured data + Qdrant for RAG/vectors
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

## Technical Standards

### Architecture Constraints
- **Service Count**: Maximum 4 services total for entire platform
- **Database Strategy**: PostgreSQL for structured data, Qdrant for semantic search/RAG
- **Communication**: Kafka for async workflows, REST for synchronous APIs
- **AI Integration**: LM Studio for all AI features (local deployment)
- **UI Strategy**: Streamlit for rapid development, natural language interfaces

### Infrastructure Integration
- **Mac Studio Services**: Leverage existing Kafka, Qdrant, PostgreSQL, Valkey, LM Studio
- **Development Machine**: MacBook M2 for code development, never run production services
- **Deployment**: rsync → Mac Studio → Docker Compose lifecycle management
- **Networking**: Use 192.168.1.10 (Mac Studio) for all service endpoints
- **Configuration**: Consul for centralized configuration management

### Development Standards
- **Spec-Driven Development**: All features start with specifications, then plans, then tasks
- **Test-Driven Development**: Write tests first, then implement to pass tests
- **Virtual Threads**: Use Java 21+ virtual threads for all I/O operations
- **Functional Programming**: Use Vavr for error handling and optional types
- **Clean Architecture**: Domain logic separate from infrastructure concerns

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

**Version**: 1.1.0 | **Ratified**: 2025-09-21 | **Last Amended**: 2025-09-23