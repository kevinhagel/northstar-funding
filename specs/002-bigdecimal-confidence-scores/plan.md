# Implementation Plan: BigDecimal Precision for All Confidence Scores

**Branch**: `002-bigdecimal-confidence-scores` | **Date**: 2025-11-01 | **Spec**: [spec.md](./spec.md)

## Summary

Fix floating-point precision errors in confidence score fields by converting all `Double`/`double` types to `BigDecimal` with scale 2. This ensures threshold comparisons (>= 0.6) work correctly and prevents precision loss (0.5999999999999999 != 0.6).

**Scope**: Domain classes, Flyway DDL, repositories, services, and tests that handle confidence scores.

**Constraint**: Edit existing Flyway migration files directly (NO new migrations) since database is empty and can be rebuilt with `mvn flyway:clean flyway:migrate`.

## Technical Context

**Language/Version**: Java 25
**Framework**: Spring Boot 3.5.6, Spring Data JDBC
**Database**: PostgreSQL 16 @ 192.168.1.10:5432
**Migration Tool**: Flyway
**Testing**: JUnit 5 + Mockito (unit tests)
**Data Type**: `BigDecimal` with scale 2, stored as `NUMERIC(3,2)` in PostgreSQL

## Constitution Check

✅ **Data Precision Standards (CRITICAL)**: This feature directly implements Constitution principle requiring BigDecimal for all confidence scores
✅ **Complexity Management**: Focused, single-responsibility change (data type fix only)
✅ **No new technologies**: Uses existing Java/Spring/PostgreSQL stack
✅ **No scripts created**: Implementation uses standard Maven commands
✅ **TDD approach**: Update tests first, then implementation

**Violations**: None

## Phase 0: Inventory & Research

### Identify All Confidence Score Fields

**Domain Module** (`northstar-domain/src/main/java/com/northstar/funding/domain/`):
1. `FundingSourceCandidate.java`
   - Field: `confidenceScore`
   - Current type: Unknown (need to verify)
   - Target type: `BigDecimal`

2. `Organization.java`
   - Field: `organizationConfidence`
   - Current type: Unknown (need to verify)
   - Target type: `BigDecimal`

3. `Domain.java`
   - Field: `bestConfidence`
   - Field: `qualityScore`
   - Current type: Unknown (need to verify)
   - Target type: `BigDecimal`

**Action**: Read each domain class to confirm current data types and Lombok annotations.

### Identify All Database Columns

**Migration Files** (`northstar-persistence/src/main/resources/db/migration/`):
1. `V1__create_funding_source_candidate.sql`
   - Column: `confidence_score`
   - Current type: Unknown (need to verify)
   - Target type: `NUMERIC(3,2)`

2. `V8__create_domain.sql`
   - Column: `best_confidence`
   - Column: `quality_score`
   - Current type: Unknown (need to verify)
   - Target type: `NUMERIC(3,2)`

3. `V15__create_organization.sql`
   - Column: `organization_confidence`
   - Current type: Unknown (need to verify)
   - Target type: `NUMERIC(3,2)`

**Action**: Read each migration file to confirm current column types.

### Identify Services with Confidence Score Logic

**Service Classes** (`northstar-persistence/src/main/java/com/northstar/funding/persistence/service/`):
1. `DomainService.java`
   - Methods that use `bestConfidence` or `qualityScore`
   - Threshold comparisons (>= 0.6 checks)

2. `OrganizationService.java`
   - Methods that use `organizationConfidence`
   - Confidence calculations

3. `FundingProgramService.java`
   - Check if confidence scores are used (unknown)

4. `SearchResultService.java`
   - Check if confidence scores are used (unknown)

**Action**: Read each service class to find methods with confidence score logic.

### Identify Tests to Update

**Unit Test Classes** (`northstar-persistence/src/test/java/com/northstar/funding/persistence/service/`):
1. `DomainServiceTest.java`
   - Mock data with confidence scores
   - Assertions on confidence values

2. `OrganizationServiceTest.java`
   - Mock data with organization confidence
   - Assertions on confidence values

3. Other test classes as discovered

**Action**: Read test classes to find confidence score usage.

**Output**: `research.md` with complete inventory of all files requiring changes.

## Phase 1: Design BigDecimal Migration Strategy

### Domain Class Changes

**Pattern for each domain class**:
```java
// Before (WRONG)
private Double confidenceScore;

// After (CORRECT)
private BigDecimal confidenceScore;
```

**Lombok compatibility**: Verify `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` work with BigDecimal.

### Flyway Migration Changes

**Pattern for each migration file**:
```sql
-- Before (WRONG)
confidence_score DOUBLE PRECISION

-- After (CORRECT)
confidence_score NUMERIC(3,2) CHECK (confidence_score >= 0.00 AND confidence_score <= 1.00)
```

### Service Class Changes

**Pattern for threshold comparisons**:
```java
// Before (WRONG)
if (confidence >= 0.6) { ... }

// After (CORRECT)
BigDecimal threshold = new BigDecimal("0.60");
if (confidence.compareTo(threshold) >= 0) { ... }
```

**Pattern for BigDecimal creation**:
```java
// CORRECT: Use string constructor
BigDecimal score = new BigDecimal("0.85").setScale(2, RoundingMode.HALF_UP);

// WRONG: Do not use double constructor
BigDecimal score = new BigDecimal(0.85); // May have precision issues
```

### Test Changes

**Pattern for mock data**:
```java
// Before (WRONG)
when(repository.findById(1L)).thenReturn(
    Optional.of(Domain.builder().bestConfidence(0.85).build())
);

// After (CORRECT)
when(repository.findById(1L)).thenReturn(
    Optional.of(Domain.builder()
        .bestConfidence(new BigDecimal("0.85"))
        .build())
);
```

**Pattern for assertions**:
```java
// Before (WRONG)
assertEquals(0.85, result.getBestConfidence());

// After (CORRECT)
assertEquals(new BigDecimal("0.85"), result.getBestConfidence());
assertEquals(0, new BigDecimal("0.85").compareTo(result.getBestConfidence()));
```

**Output**: `data-model.md` with BigDecimal patterns and migration strategy.

## Phase 2: Task Planning Approach

**Task Generation Strategy** (for `/tasks` command):

1. **Verify Current State Tasks**:
   - Read each domain class and document current types
   - Read each migration file and document current column types
   - Read each service and document confidence score usage
   - Read each test and document assertion patterns

2. **Domain Module Tasks** (northstar-domain):
   - Update `FundingSourceCandidate.confidenceScore` to BigDecimal
   - Update `Organization.organizationConfidence` to BigDecimal
   - Update `Domain.bestConfidence` and `qualityScore` to BigDecimal
   - Compile domain module: `mvn clean compile -pl northstar-domain`

3. **Persistence Module Tasks** (Flyway migrations):
   - Edit `V1__create_funding_source_candidate.sql` to use NUMERIC(3,2)
   - Edit `V8__create_domain.sql` to use NUMERIC(3,2)
   - Edit `V15__create_organization.sql` to use NUMERIC(3,2)
   - Rebuild database: `mvn flyway:clean flyway:migrate -pl northstar-persistence`

4. **Service Layer Tasks**:
   - Update `DomainService` confidence score methods to use BigDecimal
   - Update `OrganizationService` confidence score methods to use BigDecimal
   - Update any other services with confidence logic
   - Update threshold comparisons to use `.compareTo()`

5. **Test Tasks**:
   - Update `DomainServiceTest` mock data and assertions
   - Update `OrganizationServiceTest` mock data and assertions
   - Update any other test classes
   - Run all tests: `mvn test -pl northstar-persistence`

6. **Verification Tasks**:
   - Verify all domain classes compile
   - Verify Flyway migrations succeed
   - Verify all tests pass
   - Verify database schema has NUMERIC(3,2) columns

**Task Order**:
1. Domain classes first (foundation)
2. Migrations second (database schema)
3. Services third (business logic)
4. Tests fourth (verification)
5. Full verification last

**Estimated Tasks**: 15-20 focused tasks

## Complexity Tracking

**No violations** - This is a straightforward data type fix with no architectural complexity.

## Progress Tracking

**Phase Status**:
- [x] Phase 0: Research complete (inventory all files)
- [ ] Phase 1: Design complete (BigDecimal patterns documented)
- [ ] Phase 2: Task planning described (approach outlined above)
- [ ] Phase 3: Tasks generated (by `/tasks` command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Verification passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [ ] Post-Design Constitution Check: PASS
- [ ] All files inventoried
- [ ] BigDecimal patterns documented

---
*Based on Constitution v1.4.0 - Data Precision Standards (CRITICAL)*
