# Feature Specification: BigDecimal Precision for All Confidence Scores

**Feature Branch**: `002-bigdecimal-confidence-scores`
**Created**: 2025-11-01
**Status**: Draft
**Input**: User description: "Fix all confidence level fields across ALL modules (northstar-domain, northstar-persistence) to use BigDecimal with scale 2 (two decimal places). Update all domain classes, all unit tests, all integration tests, all database tables, and all Flyway migration files. Ensure floating-point precision errors are eliminated (0.5999999999999999 failing >= 0.6 threshold). Edit existing migration files directly rather than creating new migrations since database is empty and can be cleanly rebuilt."

## Execution Flow (main)
```
1. Parse user description from Input ✓
   → Fix BigDecimal precision across ALL modules
2. Extract key concepts from description ✓
   → Scope: ALL modules with confidence scores
   → Actions: Change domain classes, update tests, fix migrations
   → Constraint: Edit existing migrations, not create new ones
3. For each unclear aspect: ✓
   → Scope is comprehensive: domain, persistence, tests, database
4. Fill User Scenarios & Testing section ✓
   → Verification workflow clear
5. Generate Functional Requirements ✓
   → Each requirement testable
6. Identify affected components ✓
   → Domain classes, tests, migrations
7. Run Review Checklist ✓
   → Focused on data integrity
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT needs to be fixed and WHY
- ❌ Avoid HOW to implement
- 👥 Written for business stakeholders, not developers

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
The system calculates confidence scores throughout the funding discovery workflow. These scores determine which candidates proceed to evaluation (threshold >= 0.6). Using floating-point types causes precision errors where 0.6 becomes 0.5999999999999999, incorrectly failing threshold checks. All confidence scores across all modules must use BigDecimal with exactly 2 decimal places to ensure accurate comparisons and prevent data loss.

### Acceptance Scenarios
1. **Given** any confidence score is calculated as 0.60, **When** stored to database, **Then** it must be exactly 0.60 (not 0.5999999999999999)

2. **Given** a confidence score of 0.60 compared to threshold 0.6, **When** using BigDecimal.compareTo(), **Then** comparison returns 0 (equal) and passes >= check

3. **Given** all domain entities loaded from database, **When** confidence fields are read, **Then** all values maintain exactly 2 decimal places

4. **Given** all unit tests for confidence scoring, **When** tests run, **Then** they use BigDecimal assertions and comparisons

5. **Given** all integration tests with database, **When** tests save/load confidence scores, **Then** precision is maintained through full CRUD cycle

### Edge Cases
- What happens when calculation produces 0.5999999999999? (BigDecimal scale 2 rounds to 0.60)
- What happens with exact threshold 0.6? (BigDecimal.compareTo() correctly handles >=)
- What happens with existing tests using double assertions? (Tests must be updated to BigDecimal)
- What happens during flyway:clean flyway:migrate? (All migrations recreate tables with NUMERIC(3,2))

## Requirements *(mandatory)*

### Domain Module Requirements (northstar-domain)
- **FR-001**: FundingSourceCandidate.confidenceScore MUST be BigDecimal with scale 2
- **FR-002**: Organization.organizationConfidence MUST be BigDecimal with scale 2
- **FR-003**: Domain.bestConfidence MUST be BigDecimal with scale 2
- **FR-004**: Domain.qualityScore MUST be BigDecimal with scale 2
- **FR-005**: All Lombok @Data, @Builder annotations MUST work correctly with BigDecimal fields
- **FR-006**: All domain classes MUST compile without errors after BigDecimal changes

### Persistence Module Requirements (northstar-persistence)
- **FR-007**: All repository interfaces MUST support BigDecimal confidence fields
- **FR-008**: All service classes MUST use BigDecimal for confidence calculations
- **FR-009**: All service classes MUST use BigDecimal.compareTo() for threshold comparisons
- **FR-010**: NO service class may cast BigDecimal to double/float
- **FR-011**: All custom @Query methods MUST work with NUMERIC(3,2) columns

### Database Schema Requirements
- **FR-012**: funding_source_candidate.confidence_score column MUST be NUMERIC(3,2)
- **FR-013**: organization.organization_confidence column MUST be NUMERIC(3,2)
- **FR-014**: domain.best_confidence column MUST be NUMERIC(3,2)
- **FR-015**: domain.quality_score column MUST be NUMERIC(3,2)
- **FR-016**: All NUMERIC(3,2) columns MUST support values 0.00 to 1.00

### Flyway Migration Requirements
- **FR-017**: V1__create_funding_source_candidate.sql MUST be edited to use NUMERIC(3,2)
- **FR-018**: V8__create_domain.sql MUST be edited to use NUMERIC(3,2)
- **FR-019**: V15__create_organization.sql MUST be edited to use NUMERIC(3,2)
- **FR-020**: NO new migration files should be created
- **FR-021**: After edits, mvn flyway:clean flyway:migrate MUST succeed
- **FR-022**: After rebuild, all tables MUST have correct NUMERIC(3,2) columns

### Unit Test Requirements
- **FR-023**: All unit tests in northstar-persistence MUST use BigDecimal test data
- **FR-024**: All assertions on confidence scores MUST use BigDecimal comparisons
- **FR-025**: All mock data MUST use BigDecimal with scale 2
- **FR-026**: All unit tests MUST pass after BigDecimal changes
- **FR-027**: Tests MUST verify BigDecimal.compareTo() is used for >= threshold checks

### Integration Test Requirements (if they exist)
- **FR-028**: All integration tests MUST save/load BigDecimal confidence scores
- **FR-029**: All integration tests MUST verify precision through full CRUD cycle
- **FR-030**: All integration tests MUST pass after database schema changes

### Key Entities *(include if feature involves data)*

#### Domain Classes Requiring Updates
- **FundingSourceCandidate** (`northstar-domain/src/main/java/.../domain/FundingSourceCandidate.java`)
  - Field: `confidenceScore`
  - Change from: double/Double/float
  - Change to: BigDecimal with scale 2

- **Organization** (`northstar-domain/src/main/java/.../domain/Organization.java`)
  - Field: `organizationConfidence`
  - Change from: double/Double/float
  - Change to: BigDecimal with scale 2

- **Domain** (`northstar-domain/src/main/java/.../domain/Domain.java`)
  - Field: `bestConfidence`
  - Field: `qualityScore`
  - Change from: double/Double/float
  - Change to: BigDecimal with scale 2

#### Service Classes Requiring Updates
- **DomainService** (`northstar-persistence/src/main/java/.../persistence/service/DomainService.java`)
  - Methods using confidence scores
  - Threshold comparisons

- **OrganizationService** (`northstar-persistence/src/main/java/.../persistence/service/OrganizationService.java`)
  - Methods using organization confidence
  - Confidence calculations

- **FundingProgramService** (if confidence scores exist)
- **SearchResultService** (if confidence scores exist)

#### Migration Files Requiring Edits
- **V1__create_funding_source_candidate.sql**
  - Column: `confidence_score NUMERIC(3,2)`

- **V8__create_domain.sql**
  - Column: `best_confidence NUMERIC(3,2)`
  - Column: `quality_score NUMERIC(3,2)`

- **V15__create_organization.sql**
  - Column: `organization_confidence NUMERIC(3,2)`

#### Test Files Requiring Updates
- **Unit Tests**: All test classes in `northstar-persistence/src/test/java`
  - DomainServiceTest
  - OrganizationServiceTest
  - FundingProgramServiceTest (if applicable)

- **Integration Tests** (when created): All TestContainers tests

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details
- [x] Focused on data integrity
- [x] Written for stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers
- [x] Requirements testable
- [x] Success criteria measurable
- [x] Scope bounded (all confidence scores, all modules)
- [x] Dependencies identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (none)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
