# Feature Specification: Switch Query Generation to LM Studio

**Feature Branch**: `015-switch-query-generation`
**Created**: 2025-11-29
**Status**: Draft
**Input**: User description: "Switch query generation from Ollama to LM Studio: update LlmConfig to use port 1234, verify Perplexica uses LM Studio backend, test query generation with LM Studio, ensure 57 tests still pass"

---

## Context

Feature 014 established the search adapter infrastructure with 4 providers (Brave, SearXNG, Serper, Perplexica). During that work, we discovered that Ollama has parallelism issues when used with Perplexica despite claiming `OLLAMA_NUM_PARALLEL=10` support.

LM Studio (port 1234) has proven reliable with Perplexica in production on Mac Studio. This feature completes the transition to LM Studio as the standard LLM backend.

**Note**: LlmConfig was already refactored in a previous session (commit b62ba09). This feature verifies the integration works end-to-end and ensures Perplexica is configured correctly.

---

## User Scenarios & Testing

### Primary User Story
As a system administrator, I want the query generation system to use LM Studio instead of Ollama so that I have reliable, parallel LLM inference for search workflows.

### Acceptance Scenarios
1. **Given** LM Studio is running on Mac Studio (port 1234), **When** query generation service is invoked, **Then** queries are generated successfully using `llama-3.1-8b-instruct` model.

2. **Given** Perplexica is configured to use LM Studio, **When** Perplexica search adapter is used, **Then** AI-enhanced searches complete without LLM connection errors.

3. **Given** all 57 query generation tests, **When** test suite runs, **Then** all tests pass with LM Studio configuration.

### Edge Cases
- What happens when LM Studio is not running? System should fail fast with clear error message.
- What happens when model is not loaded? System should report model availability error.

---

## Requirements

### Functional Requirements
- **FR-001**: Query generation MUST use LM Studio at `http://192.168.1.10:1234/v1`
- **FR-002**: System MUST use `llama-3.1-8b-instruct` model for query generation
- **FR-003**: Perplexica container MUST be configured to use LM Studio (not Ollama)
- **FR-004**: All 57 existing query generation tests MUST pass
- **FR-005**: System MUST log which LLM backend is being used at startup

### Key Entities
- **LlmConfig**: Configuration for LLM connection (base URL, model, timeout, temperature)
- **Perplexica**: Docker container with `config.toml` specifying LLM backend

---

## Scope Boundaries

### In Scope
- Verify LlmConfig points to LM Studio (port 1234)
- Verify Perplexica config.toml uses LM Studio
- Run and verify all 57 tests pass
- Update docker-compose.yml if needed for Perplexica environment variables

### Out of Scope
- Two-stage LLM architecture (future feature)
- Ollama removal from Mac Studio (keep for experimentation)
- New search adapters or workflows
- Performance benchmarking

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (none found)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---

## Definition of Done

1. LlmConfig verified to use `http://192.168.1.10:1234/v1`
2. Perplexica `config.toml` verified to use LM Studio
3. All 57 query generation tests passing
4. Manual test: Generate queries and execute search workflow
5. Session summary documenting completion
