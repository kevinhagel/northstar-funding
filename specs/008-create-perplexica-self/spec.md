# Feature Specification: Perplexica Self-Hosted AI Search Infrastructure

**Feature Branch**: `008-create-perplexica-self`
**Created**: 2025-11-07
**Status**: Draft
**Input**: User description: "Create Perplexica self-hosted AI search infrastructure: Fix SearXNG configuration error (brand section strings), add Perplexica v1.11.2 container to docker-compose.yml configured to use existing SearXNG and LM Studio via native LM_STUDIO provider, create config.toml with proper LM Studio integration, deploy to Mac Studio, verify all services healthy and Perplexica UI/API working. Defers Java integration to future feature."

## Execution Flow (main)
```
1. Parse user description from Input
   ’ Infrastructure deployment: Perplexica AI search
2. Extract key concepts from description
   ’ Actors: DevOps engineer, Mac Studio infrastructure
   ’ Actions: Fix config, deploy container, verify health
   ’ Data: Docker containers, configuration files
   ’ Constraints: Use existing SearXNG, use existing LM Studio, infrastructure only
3. For each unclear aspect:
   ’ All aspects clear from description and research
4. Fill User Scenarios & Testing section
   ’ Primary: Deploy Perplexica infrastructure
5. Generate Functional Requirements
   ’ Each requirement is testable via health checks
6. Identify Key Entities (if data involved)
   ’ Infrastructure entities: containers, volumes, networks
7. Run Review Checklist
   ’ No [NEEDS CLARIFICATION] markers
   ’ No implementation details (kept infrastructure-focused)
8. Return: SUCCESS (spec ready for planning)
```

---

## ¡ Quick Guidelines
-  Focus on WHAT infrastructure needs to be deployed and WHY
- L Avoid HOW to implement (specific Docker commands deferred to plan)
- =e Written for infrastructure stakeholders

---

## User Scenarios & Testing

### Primary User Story
As a DevOps engineer, I need to deploy Perplexica AI-powered search infrastructure on Mac Studio so that the system has a self-hosted alternative to external search APIs (Tavily), reducing operational costs and maintaining privacy.

### Acceptance Scenarios

1. **Given** SearXNG has configuration errors (brand section using boolean instead of strings), **When** configuration is fixed and container is restarted, **Then** SearXNG container runs without restart loops and accepts search queries

2. **Given** Mac Studio infrastructure has PostgreSQL, Qdrant, and SearXNG running, **When** Perplexica container is added to docker-compose.yml, **Then** all services continue running without resource conflicts or network issues

3. **Given** Perplexica is configured to use existing SearXNG and LM Studio, **When** Perplexica container starts, **Then** it successfully connects to both services and responds to health checks

4. **Given** Perplexica is deployed and healthy, **When** accessing the UI at port 3000, **Then** the search interface loads and can perform AI-powered searches

5. **Given** Perplexica API endpoint is available, **When** sending search requests to /api/search, **Then** the API returns search results with AI-generated answers and citations

### Edge Cases
- What happens when LM Studio is not running? System should fail health check gracefully
- What happens when SearXNG is unavailable? Perplexica should report connection error
- How does system handle Mac Studio restart? All containers should auto-restart via docker-compose
- What happens when deploying from MacBook to Mac Studio? Rsync workflow should transfer all config files correctly

---

## Requirements

### Functional Requirements

**Configuration Management:**
- **FR-001**: System MUST fix SearXNG configuration to use empty strings instead of boolean false for brand section fields
- **FR-002**: System MUST create Perplexica configuration file (config.toml) with LM Studio provider settings
- **FR-003**: System MUST configure Perplexica to connect to existing SearXNG instance via container networking

**Container Orchestration:**
- **FR-004**: System MUST add Perplexica v1.11.2 container to docker-compose.yml
- **FR-005**: System MUST configure Perplexica container with proper volumes for data persistence and configuration mounting
- **FR-006**: System MUST configure Perplexica to use LM Studio running natively on Mac Studio via host.docker.internal
- **FR-007**: System MUST configure container dependencies so Perplexica starts only after SearXNG is healthy

**Health & Verification:**
- **FR-008**: System MUST implement health check for Perplexica container using /api/health endpoint
- **FR-009**: System MUST verify SearXNG container runs without restart loops after configuration fix
- **FR-010**: System MUST verify all existing services (PostgreSQL, Qdrant, pgAdmin) continue running after Perplexica deployment
- **FR-011**: System MUST verify Perplexica UI is accessible at port 3000
- **FR-012**: System MUST verify Perplexica API responds to search requests at /api/search endpoint

**Deployment:**
- **FR-013**: System MUST deploy all configuration and container definitions to Mac Studio at ~/northstar
- **FR-014**: System MUST use existing rsync workflow from MacBook docker/ directory to Mac Studio
- **FR-015**: System MUST document Perplexica deployment in docker infrastructure README

**Scope Boundaries:**
- **FR-016**: System MUST NOT implement Java integration with Perplexica (deferred to future feature)
- **FR-017**: System MUST NOT replace Tavily in application code (deferred to future feature)
- **FR-018**: System MUST focus on infrastructure deployment only, not application-level integration

### Key Entities

**Infrastructure Components:**
- **Perplexica Container**: AI-powered search engine service running in Docker, provides UI and API for search with LLM-enhanced answers
- **SearXNG Container**: Privacy-respecting metasearch engine, aggregates results from multiple search providers
- **LM Studio**: Local LLM inference server running natively on Mac Studio (outside Docker), provides AI capabilities via OpenAI-compatible API
- **Configuration Files**:
  - docker-compose.yml: Container orchestration definition
  - searxng/settings.yml: SearXNG search engine configuration
  - perplexica/config.toml: Perplexica search and LLM provider configuration
- **Docker Volumes**:
  - perplexica-data: Persistent data storage for Perplexica
  - perplexica-uploads: File upload storage for Perplexica
- **Network**: northstar-network bridge network enabling container-to-container communication

**Relationships:**
- Perplexica depends on SearXNG (web search provider)
- Perplexica depends on LM Studio (AI reasoning provider)
- Perplexica mounts config.toml from host filesystem
- All containers connect via northstar-network
- Perplexica exposes port 3000 for UI and API access

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (Docker commands, specific file edits deferred to plan)
- [x] Focused on infrastructure deployment value and operational needs
- [x] Written for DevOps/infrastructure stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable via health checks and API verification
- [x] Success criteria are measurable (container health, API responses, UI accessibility)
- [x] Scope is clearly bounded (infrastructure only, no Java integration)
- [x] Dependencies and assumptions identified (existing SearXNG, LM Studio, Mac Studio)

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted (infrastructure deployment, config fixes, health verification)
- [x] Ambiguities marked (none - description was clear and supported by research)
- [x] User scenarios defined (5 acceptance scenarios + 4 edge cases)
- [x] Requirements generated (18 functional requirements)
- [x] Entities identified (containers, volumes, networks, config files)
- [x] Review checklist passed

---

## Next Phase

Ready for `/plan` to generate implementation plan with detailed tasks and technical approach.
