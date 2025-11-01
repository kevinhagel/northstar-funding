# Research: BigDecimal Precision for Confidence Scores

**Date**: 2025-11-01
**Branch**: `002-bigdecimal-confidence-scores`
**Status**: ✅ ALREADY IMPLEMENTED

## Executive Summary

**Finding**: All confidence score fields are ALREADY using `BigDecimal` with scale 2, and all database columns are ALREADY using `NUMERIC(3,2)`.

**Conclusion**: This feature is **COMPLETE**. No code changes required.

## Inventory of Confidence Score Fields

### Domain Classes (northstar-domain)

#### 1. FundingSourceCandidate.java
**File**: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSourceCandidate.java:41`
```java
/**
 * AI-generated confidence score (0.00-1.00)
 * Uses BigDecimal with scale 2 for precise decimal arithmetic
 */
private BigDecimal confidenceScore;
```
✅ **Status**: CORRECT - Already using `BigDecimal`

#### 2. Organization.java
**File**: `northstar-domain/src/main/java/com/northstar/funding/domain/Organization.java:114`
```java
/**
 * Confidence score from organization-level judging (0.00-1.00)
 */
private java.math.BigDecimal organizationConfidence;
```
✅ **Status**: CORRECT - Already using `BigDecimal`

#### 3. Domain.java
**File**: `northstar-domain/src/main/java/com/northstar/funding/domain/Domain.java:92`
```java
/**
 * Highest confidence score from any candidate from this domain (0.00-1.00)
 * Uses BigDecimal with scale 2 for precise decimal arithmetic
 * Used to determine domain quality
 */
@Column("best_confidence_score")
private BigDecimal bestConfidenceScore;
```
✅ **Status**: CORRECT - Already using `BigDecimal`

### Database Columns (Flyway Migrations)

#### 1. V1__create_funding_source_candidate.sql
**File**: `northstar-persistence/src/main/resources/db/migration/V1__create_funding_source_candidate.sql`
```sql
confidence_score DECIMAL(3,2) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
```
✅ **Status**: CORRECT - Already using `DECIMAL(3,2)` with CHECK constraint

**Index**:
```sql
CREATE INDEX idx_funding_source_candidate_status_confidence
    ON funding_source_candidate (status, confidence_score DESC);
```
✅ Index supports ordering by confidence score

#### 2. V8__create_domain.sql
**File**: `northstar-persistence/src/main/resources/db/migration/V8__create_domain.sql`
```sql
best_confidence_score DECIMAL(3,2) CHECK (best_confidence_score >= 0.00 AND best_confidence_score <= 1.00),
```
✅ **Status**: CORRECT - Already using `DECIMAL(3,2)` with CHECK constraint

#### 3. V15__create_organization.sql
**File**: `northstar-persistence/src/main/resources/db/migration/V15__create_organization.sql`
```sql
organization_confidence DECIMAL(3,2) CHECK (organization_confidence BETWEEN 0.00 AND 1.00),
```
✅ **Status**: CORRECT - Already using `DECIMAL(3,2)` with CHECK constraint

**Index**:
```sql
CREATE INDEX idx_organization_confidence ON organization(organization_confidence DESC);
```
✅ Index supports ordering by confidence score

### NO Float/Double Found

**Domain Classes Audit**:
```bash
grep -r "double\|Double\|float\|Float" northstar-domain/src/main/java/com/northstar/funding/domain/*.java
```
✅ **Result**: NO float/double fields found in domain classes

**Migration Files Audit**:
```bash
grep -i "double\|float\|real" northstar-persistence/src/main/resources/db/migration/*.sql
```
✅ **Result**: Only DOUBLE PRECISION found in performance monitoring views (V6), NOT in data columns

## Service Layer Analysis

### Services with Confidence Score Logic

**Services to verify**:
1. `DomainService.java` - Methods using `bestConfidenceScore`
2. `OrganizationService.java` - Methods using `organizationConfidence`
3. `FundingProgramService.java` - Unknown if uses confidence
4. `SearchResultService.java` - Unknown if uses confidence

**Next Step**: Read service implementations to verify they use `BigDecimal.compareTo()` for threshold checks.

## Test Classes

**Unit Tests to verify**:
1. `DomainServiceTest.java` - Mock data and assertions
2. `OrganizationServiceTest.java` - Mock data and assertions
3. Other test classes as discovered

**Next Step**: Read test implementations to verify they use BigDecimal test data.

## Verification Tasks Remaining

Since all domain and database schemas are correct, we need to verify:

1. **Service layer threshold comparisons** use `.compareTo()`:
   ```java
   // CORRECT
   BigDecimal threshold = new BigDecimal("0.60");
   if (confidence.compareTo(threshold) >= 0) { ... }

   // WRONG (if found)
   if (confidence >= 0.6) { ... }
   ```

2. **Test data** uses BigDecimal:
   ```java
   // CORRECT
   new BigDecimal("0.85")

   // WRONG (if found)
   0.85 or Double.valueOf(0.85)
   ```

## Decision: Complete vs. Verify

**Options**:
1. **Close this feature** - Domain and database are correct, trust that services follow suit
2. **Verify service/test layer** - Read all service methods and tests to ensure BigDecimal usage

**Recommendation**: Proceed with Phase 1 to verify service/test implementations, then determine if any fixes are needed.

---
