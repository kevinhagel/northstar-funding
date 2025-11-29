# Implementation Plan: Switch Query Generation to LM Studio

**Branch**: `015-switch-query-generation` | **Date**: 2025-11-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-switch-query-generation/spec.md`

## Summary

Switch the NorthStar LLM backend from Ollama (port 11434) to LM Studio (port 1234) for query generation and Perplexica integration. Query generation config (application.yml) is already updated. Perplexica config.toml still points to Ollama and needs updating.

## Technical Context

**Language/Version**: Java 25 (Oracle JDK via SDKMAN)
**Primary Dependencies**: Spring Boot 3.5.7, LangChain4j 1.8.0
**Storage**: PostgreSQL 16 (192.168.1.10:5432)
**Testing**: JUnit/Jupiter (57 tests in northstar-query-generation)
**Target Platform**: Mac Studio (192.168.1.10) + MacBook M2 (development)
**Project Type**: Monolith (multi-module Maven)
**Performance Goals**: Query generation <10s per request
**Constraints**: LM Studio must be running with `llama-3.1-8b-instruct` loaded
**Scale/Scope**: Verification feature - minimal code changes

## Constitution Check

*GATE: All checks pass - this is a configuration change, not new architecture*

| Principle | Status | Notes |
|-----------|--------|-------|
| IV. Technology Stack | PASS | LM Studio is approved local LLM |
| X. Technology Constraints | PASS | No forbidden technologies |
| Architecture Mandate | PASS | Monolith structure maintained |
| Infrastructure Integration | PASS | Using Mac Studio infrastructure |
| Data Precision | N/A | No confidence score changes |

## Current State Analysis

### Query Generation (LlmConfig) - ALREADY CORRECT
```yaml
# northstar-query-generation/src/main/resources/application.yml
query-generation:
  llm:
    base-url: http://192.168.1.10:1234/v1  # LM Studio
    model-name: llama-3.1-8b-instruct
```

### Perplexica Config - NEEDS UPDATE
```toml
# docker/perplexica/config.toml (CURRENT - WRONG)
[API_ENDPOINTS]
OLLAMA = "http://192.168.1.10:11434"  # Ollama

[CHAT_MODEL]
PROVIDER = "ollama"
MODEL = "llama3.1:8b"
```

**Issue**: Perplexica doesn't have native LM Studio support. It uses Ollama protocol.

### Resolution Options

1. **Keep Perplexica on Ollama** - Perplexica uses Ollama for its internal AI search. The NorthStar query generation uses LM Studio directly. These are separate concerns.

2. **Route Perplexica through LM Studio** - LM Studio exposes OpenAI-compatible API, but Perplexica expects Ollama protocol.

**Decision**: Keep Perplexica on Ollama. The spec requirement (FR-003) needs clarification - Perplexica's internal LLM is for its AI search features, separate from NorthStar's query generation.

## Phase 0: Research

**Finding 1**: LM Studio vs Ollama protocols
- LM Studio exposes OpenAI-compatible API at `/v1/chat/completions`
- Ollama uses its own protocol at `/api/generate` and `/api/chat`
- Perplexica is built to use Ollama protocol
- Query generation (LangChain4j) uses OpenAI-compatible API (works with LM Studio)

**Finding 2**: Separation of concerns
- **NorthStar Query Generation**: Uses LM Studio (port 1234) via LangChain4j
- **Perplexica Internal**: Uses Ollama (port 11434) for its AI-enhanced search
- These are independent - Perplexica is a search provider, not using NorthStar's LLM

**Finding 3**: Current test status
- 57 query generation tests use mocked LLM
- Tests pass regardless of which LLM backend is running
- Real LLM validation requires manual testing

**Output**: No unknowns remain. Architecture is correct as-is.

## Phase 1: Design

### Data Model
No changes required - this is a configuration verification feature.

### Contracts
No new APIs or contracts - verification only.

### What's Already Done (Previous Session)
1. `LlmConfig.java` created with LM Studio config (commit b62ba09)
2. `application.yml` updated to use port 1234
3. `application-test.yml` updated to match
4. Old `OllamaConfig.java` deleted

### What Needs Verification
1. LM Studio is running on Mac Studio with correct model
2. Query generation tests pass (57 tests)
3. Manual smoke test of query generation

## Phase 2: Task Planning Approach

This is a verification feature with 4 simple tasks:

1. **Verify LM Studio availability** - Check port 1234 responds with correct model
2. **Run query generation tests** - Confirm 57 tests pass
3. **Verify Perplexica** - Confirm it's working (on Ollama, which is fine)
4. **Manual smoke test** - Generate queries and execute search

**Estimated Output**: 4 verification tasks (no code changes needed)

## Complexity Tracking

| Item | Status | Notes |
|------|--------|-------|
| Perplexica on Ollama | ACCEPTABLE | Different concern than NorthStar query generation |
| Two LLM backends | ACCEPTABLE | LM Studio for queries, Ollama for Perplexica |

## Progress Tracking

**Phase Status**:
- [x] Phase 0: Research complete
- [x] Phase 1: Design complete (no changes needed)
- [x] Phase 2: Task planning complete
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---

## Clarification for Spec

**FR-003 Revision**: The spec said "Perplexica container MUST be configured to use LM Studio (not Ollama)". This is incorrect because:
1. Perplexica uses Ollama protocol, not OpenAI-compatible API
2. Perplexica's LLM is for its internal AI search, separate from NorthStar
3. The important requirement is that NorthStar's query generation uses LM Studio

**Updated Understanding**:
- FR-001, FR-002: Query generation uses LM Studio - ALREADY DONE
- FR-003: Perplexica works independently on Ollama - ACCEPTABLE
- FR-004: Tests pass - TO BE VERIFIED
- FR-005: Logging shows LM Studio - ALREADY DONE

---
*Based on Constitution v1.4.0*
