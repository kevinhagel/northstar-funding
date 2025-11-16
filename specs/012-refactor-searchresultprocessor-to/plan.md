# Implementation Plan: SearchResultProcessor Refactoring

**Branch**: `012-refactor-searchresultprocessor-to` | **Date**: 2025-11-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/012-refactor-searchresultprocessor-to/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path ✅
   → Feature loaded: SearchResultProcessor refactoring (8 objectives)
2. Fill Technical Context (scan for NEEDS CLARIFICATION) ✅
   → No NEEDS CLARIFICATION - all requirements explicit
   → Project Type: single (existing Java monolith)
   → Structure Decision: Use existing multi-module Maven structure
3. Fill Constitution Check section ✅
   → Based on constitution v1.4.0
4. Evaluate Constitution Check section
   → No violations - pure refactoring within existing module
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → Research refactoring patterns and testing strategies
6. Execute Phase 1 → contracts, data-model.md, quickstart.md
   → ProcessingContext model design
   → Pipeline stage contracts
   → Test scenarios for validation
7. Re-evaluate Constitution Check section
   → Verify compliance after design
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 8. Phase 2 is executed by /tasks command.

## Summary

**Primary Requirement**: Refactor SearchResultProcessor to fix critical bugs (lowConfidenceCreated tracking, missing spam TLD filtering) and improve code quality (testability, maintainability, observability) without changing functionality.

**Technical Approach**:
- Extract pipeline stages into independently testable methods
- Introduce ProcessingContext domain object to eliminate primitive obsession
- Implement spam TLD filtering before deduplication
- Add structured logging at each pipeline stage
- Fix statistics tracking bugs
- All changes validated by existing + new unit tests in TestContainers environment

## Technical Context

**Language/Version**: Java 25 (Oracle JDK via SDKMAN)
**Primary Dependencies**:
- Spring Boot 3.5.7 (existing)
- Spring Data JDBC (existing)
- Vavr 0.10.7 (functional error handling - available but not yet used in this class)
- SLF4J + Logback (logging framework)
- TestContainers 1.21.3 (integration testing - Feature 011 infrastructure)

**Storage**: PostgreSQL 16 (existing - no schema changes required)
**Testing**:
- JUnit 5 + Mockito (unit tests - 7 existing SearchResultProcessorTest scenarios)
- TestContainers (integration tests - verifying refactored code against real database)
- AssertJ (fluent assertions)

**Target Platform**: Spring Boot application on JVM
**Project Type**: single (existing multi-module Maven monolith: northstar-crawler module)
**Performance Goals**:
- No performance regression (existing: <100ms for 100 results)
- Duplicate detection optimization: single HashSet operation vs current two operations
- Test execution: existing 7 tests must complete in <5s (no slowdown)

**Constraints**:
- Zero functionality changes (backward compatibility required)
- Public API unchanged: `processSearchResults(List<SearchResult>, UUID)` signature preserved
- ProcessingStatistics field structure unchanged
- All existing 7 unit tests must pass without modification
- No new external dependencies (use existing Spring/Java libraries)

**Scale/Scope**:
- Single class refactoring: SearchResultProcessor.java (~142 lines)
- Extract ~6 pipeline stage methods
- Create 1 domain object: ProcessingContext
- Add ~8-10 new unit tests for extracted methods
- Update 0 integration tests (Feature 011 infrastructure ready for validation)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: XML Tag Accuracy (CRITICAL)
**Status**: N/A - No XML changes in this refactoring

### Principle II: Domain-Driven Design (UBIQUITOUS LANGUAGE)
**Status**: ✅ COMPLIANT
- ProcessingContext follows DDD principles (domain object encapsulating processing state)
- No language changes to existing domain (SearchResult, ProcessingStatistics)
- Refactoring preserves funding discovery workflow terminology

### Principle III: Human-AI Collaboration (MANDATORY)
**Status**: ✅ COMPLIANT
- Improved observability (structured logging) enables human monitoring
- Enhanced testability allows developers to validate pipeline behavior
- No changes to human workflow integration

### Principle IV: Technology Stack (NON-NEGOTIABLE)
**Status**: ✅ COMPLIANT
- Java 25 source and target level (unchanged)
- Spring Boot 3.5.7 (unchanged)
- No new external dependencies
- Uses existing SLF4J logging (part of Spring Boot)
- TestContainers 1.21.3 (Feature 011 infrastructure ready)

### Principle V: Three-Workflow Architecture
**Status**: ✅ COMPLIANT
- Refactoring within Funding Discovery workflow
- No cross-workflow changes

### Principle VI: Complexity Management (ESSENTIAL)
**Status**: ✅ COMPLIANT - **Reduces Complexity**
- Breaks 82-line method into focused, single-responsibility stage methods
- ProcessingContext eliminates scattered primitive state management
- Improves testability (independent stage testing vs current monolithic testing)
- No new classes beyond ProcessingContext helper

### Principle VII: Contact Intelligence Priority
**Status**: N/A - No contact intelligence changes in this refactoring

### Principle VIII: Deployment Responsibility (NON-NEGOTIABLE)
**Status**: ✅ COMPLIANT
- Code-only changes, no deployment scripts
- No rsync operations

### Principle IX: Script Creation Permission (MANDATORY)
**Status**: ✅ COMPLIANT
- No script creation in this refactoring

### Principle X: Technology Constraints - Lessons from Spring-Crawler (CRITICAL)
**Status**: ✅ COMPLIANT
- Uses approved technologies: Spring, Lombok, Vavr (optional), JUnit
- No forbidden technologies (crawl4j, langgraph4j, langchain4j, microservices)
- Maintains monolith architecture
- Simple refactoring, no over-engineering

### Principle XI: Two Web Layers - Separate Concerns (ARCHITECTURAL)
**Status**: N/A - No web layer changes in this refactoring

### Data Precision Standards (CRITICAL)
**Status**: ✅ COMPLIANT
- Existing code already uses BigDecimal for confidence scores
- Refactoring preserves BigDecimal usage
- No precision-related changes

### Architecture Constraints
**Status**: ✅ COMPLIANT
- Monolith architecture (northstar-crawler module)
- No new modules or microservices
- Clear package separation maintained

### Infrastructure Integration
**Status**: ✅ COMPLIANT
- No infrastructure changes
- PostgreSQL 16 integration unchanged
- TestContainers used for validation (Feature 011 ready)

### Development Standards
**Status**: ✅ COMPLIANT
- Spec-Driven Development: This feature spec → plan → tasks
- Test-Driven Development: New tests for extracted methods
- Integration Testing: TestContainers validation
- No Virtual Threads needed (synchronous processing)
- Vavr optional (can use for error handling if beneficial)
- Clean Architecture: Domain logic separation maintained

**Constitution Compliance Summary**: ✅ **FULL COMPLIANCE** - Pure refactoring with complexity reduction

## Project Structure

### Documentation (this feature)
```
specs/012-refactor-searchresultprocessor-to/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (to be generated)
├── data-model.md        # Phase 1 output (ProcessingContext model)
├── quickstart.md        # Phase 1 output (test validation scenarios)
├── contracts/           # Phase 1 output (pipeline stage contracts)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (existing repository structure)
```
northstar-crawler/src/main/java/com/northstar/funding/crawler/
├── processing/
│   ├── SearchResultProcessor.java     # To be refactored
│   ├── ProcessingStatistics.java      # Existing (enhanced with invalidUrlsSkipped)
│   ├── SearchResult.java              # Existing (unchanged)
│   └── ProcessingContext.java         # NEW domain object
└── scoring/
    ├── ConfidenceScorer.java           # Existing (unchanged)
    ├── DomainCredibilityService.java   # Existing (uses spam TLD filtering)
    └── CandidateCreationService.java   # Existing (unchanged)

northstar-crawler/src/test/java/com/northstar/funding/crawler/
├── processing/
│   ├── SearchResultProcessorTest.java      # Existing (7 tests - must pass)
│   ├── ProcessingContextTest.java          # NEW unit tests
│   └── PipelineStageTest.java              # NEW unit tests for extracted methods
└── scoring/
    └── (existing scorer tests - unchanged)
```

**Structure Decision**: Use existing multi-module Maven structure (northstar-crawler module)

## Phase 0: Outline & Research

**No NEEDS CLARIFICATION in Technical Context** - All requirements explicit in feature spec.

### Research Tasks

1. **Refactoring Patterns Research**:
   - Extract Method refactoring pattern
   - Replace Primitive with Object refactoring
   - Strategy pattern for pipeline stages (optional)
   - Best practices for maintaining backward compatibility during refactoring

2. **Testing Strategy Research**:
   - TestContainers best practices for refactoring validation
   - JUnit 5 parameterized tests for pipeline stages
   - Mockito best practices for testing extracted methods
   - AssertJ fluent assertions for clearer test readability

3. **Logging Patterns Research**:
   - SLF4J structured logging patterns
   - MDC (Mapped Diagnostic Context) for session ID correlation
   - Log level best practices (INFO vs WARN vs DEBUG)
   - Performance impact of logging in hot paths

4. **BigDecimal Best Practices**:
   - Verify existing BigDecimal usage in SearchResultProcessor
   - Scale and rounding mode consistency
   - Comparison patterns (.compareTo vs equals)

**Output**: research.md with refactoring approach, testing strategy, logging patterns, and BigDecimal verification

## Phase 1: Design & Contracts

### 1. Data Model Design (`data-model.md`)

**ProcessingContext** (new domain object):
```
Entity: ProcessingContext
Purpose: Encapsulates all processing state for a single search result batch

Fields:
- sessionId: UUID (for logging correlation)
- confidenceThreshold: BigDecimal (0.60 constant)
- seenDomains: Set<String> (deduplication tracking)
- spamTldFiltered: int (counter)
- blacklistedSkipped: int (counter)
- duplicatesSkipped: int (counter)
- highConfidenceCreated: int (counter)
- lowConfidenceCreated: int (counter)
- invalidUrlsSkipped: int (new counter)

Methods:
- markDomainAsSeen(String domain): boolean (returns false if duplicate)
- recordSpamTldFiltered(): void
- recordBlacklisted(): void
- recordLowConfidence(): void
- recordHighConfidence(): void
- recordInvalidUrl(): void
- buildStatistics(int totalResults): ProcessingStatistics
```

**ProcessingStatistics** (enhanced existing model):
```
Entity: ProcessingStatistics
Purpose: Immutable summary of processing outcomes

NEW Field:
- invalidUrlsSkipped: int (add to existing fields)

Existing Fields (unchanged):
- totalResults: int
- spamTldFiltered: int
- blacklistedSkipped: int
- duplicatesSkipped: int
- highConfidenceCreated: int
- lowConfidenceCreated: int

Derived Fields (unchanged):
- totalCandidatesCreated(): int
- totalProcessed(): int
```

### 2. Pipeline Stage Contracts (`contracts/`)

**Contract 1: Domain Extraction Stage**
```
Input: SearchResult, ProcessingContext
Output: Optional<String> domain
Decision: Continue (domain present) | Skip (domain absent, record invalid URL)
Logging: WARN if extraction fails
```

**Contract 2: Spam TLD Filtering Stage**
```
Input: SearchResult (URL), ProcessingContext
Output: boolean isSpam
Decision: Skip (spam TLD, record filtered) | Continue (not spam)
Logging: INFO if filtered
```

**Contract 3: Deduplication Stage**
```
Input: String domain, ProcessingContext
Output: boolean isDuplicate
Decision: Skip (duplicate, record) | Continue (unique)
Logging: DEBUG if duplicate
```

**Contract 4: Blacklist Checking Stage**
```
Input: String domain, ProcessingContext
Output: boolean isBlacklisted
Decision: Skip (blacklisted, record) | Continue (allowed)
Logging: WARN if blacklisted
```

**Contract 5: Confidence Scoring Stage**
```
Input: SearchResult, ProcessingContext
Output: BigDecimal confidenceScore
Decision: Continue to threshold check
Logging: DEBUG with calculated score
```

**Contract 6: Threshold Filtering Stage**
```
Input: BigDecimal confidence, ProcessingContext
Output: boolean meetsThreshold
Decision: Skip (low, record) | Continue (high, record)
Logging: DEBUG with decision
```

**Contract 7: Candidate Creation Stage**
```
Input: SearchResult, Domain, BigDecimal confidence, ProcessingContext
Output: FundingSourceCandidate (created and saved)
Decision: Candidate created successfully
Logging: INFO with candidate ID
```

### 3. Quickstart Test Scenarios (`quickstart.md`)

**Scenario 1**: Run existing 7 SearchResultProcessorTest tests
- Expected: All pass unchanged (backward compatibility)

**Scenario 2**: Run new ProcessingContextTest
- Expected: State management tests pass

**Scenario 3**: Run new PipelineStageTest
- Expected: Independent stage tests pass

**Scenario 4**: Run integration tests with TestContainers
- Expected: Refactored code works against real database

**Scenario 5**: Performance validation
- Process 1000 search results
- Expected: Duplicate detection optimization shows improvement

### 4. Agent File Update

Will execute `.specify/scripts/bash/update-agent-context.sh claude` to update CLAUDE.md with:
- SearchResultProcessor refactoring context
- ProcessingContext domain object
- Pipeline stage extraction approach
- Keep under 150 lines (O(1) update)

**Output**: data-model.md, contracts/, quickstart.md, updated CLAUDE.md

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:

1. **Load tasks template** from `.specify/templates/tasks-template.md`

2. **Generate tasks from Phase 1 design**:

   **Infrastructure Tasks** (Phase A - Sequential):
   - Create ProcessingContext class with state management
   - Add invalidUrlsSkipped field to ProcessingStatistics
   - Extract CONFIDENCE_THRESHOLD constant
   - Add SLF4J logger to SearchResultProcessor

   **Refactoring Tasks** (Phase B - Sequential, TDD order):
   - Extract domain extraction method + write tests [TDD]
   - Extract spam TLD filtering method + write tests [TDD]
   - Extract deduplication method + write tests [TDD]
   - Extract blacklist checking method + write tests [TDD]
   - Extract confidence threshold check + write tests [TDD]
   - Extract candidate creation method + write tests [TDD]

   **Integration Tasks** (Phase C - Sequential):
   - Refactor processSearchResults to use pipeline stages
   - Add structured logging to each stage
   - Fix lowConfidenceCreated tracking bug
   - Optimize duplicate detection (use Set.add directly)

   **Validation Tasks** (Phase D - Sequential):
   - Run existing SearchResultProcessorTest (verify 7 tests pass)
   - Run new ProcessingContextTest
   - Run new PipelineStageTest
   - Run integration tests with TestContainers
   - Performance validation (1000 results benchmark)

3. **Ordering Strategy**:
   - TDD order: Write test → extract method → make test pass
   - Dependency order: ProcessingContext → pipeline stages → integration
   - Mark [P] where applicable (ProcessingContext tests can run parallel to stage tests)

4. **Estimated Task Count**: 22-25 tasks
   - Phase A: 4 tasks (infrastructure setup)
   - Phase B: 12 tasks (6 stages × 2 tasks each: test + implementation)
   - Phase C: 4 tasks (integration and bug fixes)
   - Phase D: 5 tasks (validation and performance)

**IMPORTANT**: Tasks will be generated by /tasks command with complete acceptance criteria and file paths.

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md with 22-25 tasks)
**Phase 4**: Implementation (execute tasks.md following TDD and constitutional principles)
**Phase 5**: Validation (run all tests, verify backward compatibility, performance check)

## Complexity Tracking
*No constitutional violations - this section intentionally left empty*

This refactoring **reduces complexity** per Principle VI:
- Breaks 82-line method into focused single-responsibility methods
- Eliminates primitive obsession with ProcessingContext
- Improves testability (unit tests for individual stages)
- No new external dependencies
- No architectural changes

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) - COMPLETE ✅
- [x] Phase 1: Design complete (/plan command) - COMPLETE ✅
  - ✅ data-model.md generated (ProcessingContext, enhanced ProcessingStatistics)
  - ✅ contracts/pipeline-stages.md generated (7 stage contracts with tests)
  - ✅ quickstart.md generated (8 validation scenarios)
  - ✅ CLAUDE.md updated via update-agent-context.sh
- [x] Phase 2: Task planning complete (/plan command - describe approach only) - APPROACH DEFINED ✅
- [ ] Phase 3: Tasks generated (/tasks command) - READY TO START 🚀
- [ ] Phase 4: Implementation complete - NOT STARTED
- [ ] Phase 5: Validation passed - NOT STARTED

**Gate Status**:
- [x] Initial Constitution Check: PASS (full compliance, reduces complexity)
- [x] Post-Design Constitution Check: PASS (design verified, no new violations)
- [x] All NEEDS CLARIFICATION resolved (none existed)
- [x] Complexity deviations documented (none - this refactoring reduces complexity)

---
*Based on Constitution v1.4.0 - See `.specify/memory/constitution.md`*
