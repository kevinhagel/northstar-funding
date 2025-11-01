# TDD Requirements Analysis: Integration Tests

## Executive Summary

The integration tests define requirements that are **not yet implemented** in the current codebase. This is proper TDD - tests define the contract first. Below is a complete analysis of what needs to be added/changed.

---

## 1. Database Schema Changes

### 1.1 FundingSourceCandidate Table (V1__create_funding_source_candidate.sql)

**ADD these columns:**

```sql
-- Approval tracking
approved_by UUID NULL,  -- References admin_user(admin_user_id)
approved_at TIMESTAMPTZ NULL,
rejected_by UUID NULL,  -- References admin_user(admin_user_id)
rejected_at TIMESTAMPTZ NULL,

-- Foreign key constraints
CONSTRAINT funding_source_candidate_approved_by_fk
    FOREIGN KEY (approved_by) REFERENCES admin_user(admin_user_id) ON DELETE SET NULL,
CONSTRAINT funding_source_candidate_rejected_by_fk
    FOREIGN KEY (rejected_by) REFERENCES admin_user(admin_user_id) ON DELETE SET NULL
```

**Rationale:** Tests track WHO approved/rejected candidates and WHEN, not just the status change.

---

### 1.2 EnhancementRecord Table (V5__create_enhancement_record.sql)

**CURRENT SCHEMA:**
```sql
field_name VARCHAR(100) NOT NULL,
old_value TEXT,
new_value TEXT,
enhancement_type VARCHAR(50) NOT NULL
```

**REQUIRED SCHEMA for AI tracking:**
```sql
-- Core fields
field_name VARCHAR(100) NOT NULL,
original_value TEXT,       -- Renamed from old_value
suggested_value TEXT,      -- Renamed from new_value
enhancement_type VARCHAR(50) NOT NULL,

-- AI tracking fields (NEW)
ai_model VARCHAR(100),              -- LM Studio model used (e.g., "llama-3.1-8b")
confidence_score DECIMAL(3,2),      -- AI confidence (0.00-1.00)
human_approved BOOLEAN DEFAULT FALSE,
approved_by UUID,                   -- References admin_user(admin_user_id)
approved_at TIMESTAMPTZ,

-- Foreign key
CONSTRAINT enhancement_record_approved_by_fk
    FOREIGN KEY (approved_by) REFERENCES admin_user(admin_user_id) ON DELETE SET NULL,

-- CHECK constraints
CONSTRAINT enhancement_record_enhancement_type_check
    CHECK (enhancement_type IN ('AI_SUGGESTED', 'MANUAL', 'HUMAN_MODIFIED')),

CONSTRAINT enhancement_record_ai_fields_consistency
    CHECK (
        (enhancement_type = 'AI_SUGGESTED' AND ai_model IS NOT NULL) OR
        (enhancement_type != 'AI_SUGGESTED' AND ai_model IS NULL)
    )
```

**Rationale:** Tests track AI suggestions vs human decisions - core constitutional requirement for human-AI collaboration.

---

### 1.3 Migration Strategy

**Option A: Create new migration (RECOMMENDED)**
```
V7__add_approval_tracking.sql  - Add approval/rejection tracking to funding_source_candidate
V8__enhance_enhancement_record.sql - Rebuild enhancement_record with AI tracking
```

**Option B: Modify existing migrations** (only if database not yet deployed to production)

---

## 2. Domain Model Changes

### 2.1 FundingSourceCandidate.java

**ADD these fields:**

```java
// Approval workflow tracking
@Column("approved_by")
private UUID approvedBy;

@Column("approved_at")
private LocalDateTime approvedAt;

@Column("rejected_by")
private UUID rejectedBy;

@Column("rejected_at")
private LocalDateTime rejectedAt;

// ADD getters/setters (Lombok @Data will auto-generate)
```

**CURRENT:** 89 lines (ends at line 89)
**AFTER:** ~97 lines (8 new lines)

---

### 2.2 EnhancementType.java (enum)

**REPLACE current enum values:**

**CURRENT:**
```java
public enum EnhancementType {
    CONTACT_ADDED,
    DATA_CORRECTED,
    NOTES_ADDED,
    DUPLICATE_MERGED,
    STATUS_CHANGED,
    VALIDATION_COMPLETED
}
```

**REQUIRED:**
```java
public enum EnhancementType {
    /**
     * AI suggested this enhancement but human hasn't approved yet
     */
    AI_SUGGESTED,

    /**
     * Human manually created this enhancement without AI assistance
     */
    MANUAL,

    /**
     * Human modified an AI suggestion before accepting
     */
    HUMAN_MODIFIED
}
```

**BREAKING CHANGE:** This changes the semantic meaning from "what changed" to "who/how changed".

**Alternative:** Keep both enums:
- `EnhancementType` - who/how (AI_SUGGESTED, MANUAL, HUMAN_MODIFIED)
- `EnhancementCategory` - what (CONTACT_ADDED, DATA_CORRECTED, etc.)

---

### 2.3 EnhancementRecord.java

**Major refactoring required:**

**CURRENT fields:**
```java
private String fieldName;
private String oldValue;
private String newValue;
private EnhancementType enhancementType;
private UUID enhancedBy;
private LocalDateTime enhancedAt;
private String notes;
private Integer timeSpentMinutes;
private String reviewComplexity;
```

**REQUIRED fields:**
```java
private String fieldName;
private String originalValue;        // Renamed from oldValue
private String suggestedValue;       // Renamed from newValue
private EnhancementType enhancementType;
private UUID enhancedBy;
private LocalDateTime enhancedAt;
private String notes;
private Integer timeSpentMinutes;
private String reviewComplexity;

// NEW AI tracking fields
private String aiModel;              // LM Studio model (e.g., "llama-3.1-8b")
private Double confidenceScore;      // AI confidence (0.0-1.0)
private Boolean humanApproved;       // Did human approve AI suggestion?
private UUID approvedBy;             // Who approved
private LocalDateTime approvedAt;    // When approved
```

**ADD builder pattern:**
```java
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancementRecord {
    // ... fields
}
```

**CHANGE constructor:**
- Remove custom constructors
- Use Lombok @Builder, @NoArgsConstructor, @AllArgsConstructor

**CURRENT:** 228 lines
**AFTER:** ~250 lines (22 new lines for AI tracking)

---

## 3. Repository Changes

### 3.1 FundingSourceCandidateRepository.java

**ADD this method:**

```java
/**
 * Find all candidates by status
 * Used by integration tests to query candidates in specific workflow states
 */
List<FundingSourceCandidate> findByStatus(CandidateStatus status);
```

**Location:** After line 48 (after `findByStatusOrderByConfidenceScoreDesc`)

---

### 3.2 EnhancementRecordRepository.java

**ADD this method:**

```java
/**
 * Find all enhancement records for a specific candidate
 * Used for audit trail and enhancement history tracking
 */
List<EnhancementRecord> findByCandidateId(UUID candidateId);

/**
 * Find enhancements by type (AI_SUGGESTED, MANUAL, HUMAN_MODIFIED)
 * Used for analyzing AI vs human contribution patterns
 */
List<EnhancementRecord> findByEnhancementType(EnhancementType enhancementType);
```

**Location:** Add to existing repository interface

---

## 4. Service Layer Changes

### 4.1 CandidateValidationService.java

**CHANGE method signatures:**

**CURRENT:**
```java
public FundingSourceCandidate approveCandidate(UUID candidateId, String approvalNotes)

public FundingSourceCandidate rejectCandidate(UUID candidateId, String rejectionReason)
```

**REQUIRED:**
```java
public FundingSourceCandidate approveCandidate(UUID candidateId, UUID adminUserId, String approvalNotes)

public FundingSourceCandidate rejectCandidate(UUID candidateId, UUID adminUserId, String rejectionReason)
```

**IMPLEMENTATION CHANGES:**

```java
public FundingSourceCandidate approveCandidate(UUID candidateId, UUID adminUserId, String approvalNotes) {
    // ... existing validation ...

    candidate.setStatus(CandidateStatus.APPROVED);
    candidate.setApprovedBy(adminUserId);        // NEW
    candidate.setApprovedAt(LocalDateTime.now()); // NEW
    candidate.setValidationNotes(approvalNotes);

    return candidateRepository.save(candidate);
}

public FundingSourceCandidate rejectCandidate(UUID candidateId, UUID adminUserId, String rejectionReason) {
    // ... existing validation ...

    candidate.setStatus(CandidateStatus.REJECTED);
    candidate.setRejectedBy(adminUserId);         // NEW
    candidate.setRejectedAt(LocalDateTime.now());  // NEW
    candidate.setRejectionReason(rejectionReason);

    return candidateRepository.save(candidate);
}
```

**Rationale:** Tests expect to track WHO approved/rejected, not just the status.

---

### 4.2 CandidateValidationServiceTest.java (Unit Test)

**UPDATE test calls** to match new signatures:

```java
// OLD:
validationService.approveCandidate(candidateId, "notes");

// NEW:
UUID adminUser = UUID.randomUUID();
validationService.approveCandidate(candidateId, adminUser, "notes");
```

**Files to update:**
- `CandidateValidationServiceTest.java` - All test methods calling approve/reject

---

## 5. Summary of Changes

### Database (2 new migrations)
- ✅ V7: Add `approved_by`, `approved_at`, `rejected_by`, `rejected_at` to `funding_source_candidate`
- ✅ V8: Add AI tracking fields to `enhancement_record`, rename `old_value`→`original_value`, `new_value`→`suggested_value`

### Domain Models (3 files)
- ✅ `FundingSourceCandidate.java` - Add 4 approval/rejection tracking fields
- ✅ `EnhancementType.java` - Replace enum values (AI_SUGGESTED, MANUAL, HUMAN_MODIFIED)
- ✅ `EnhancementRecord.java` - Add 5 AI tracking fields, add @Builder, rename old/new value fields

### Repositories (2 files)
- ✅ `FundingSourceCandidateRepository.java` - Add `findByStatus()` method
- ✅ `EnhancementRecordRepository.java` - Add `findByCandidateId()` and `findByEnhancementType()` methods

### Services (2 files)
- ✅ `CandidateValidationService.java` - Change `approveCandidate()` and `rejectCandidate()` signatures (add `adminUserId` parameter)
- ✅ `CandidateValidationServiceTest.java` - Update all test calls to new signatures

---

## 6. Implementation Order (TDD Approach)

1. **Database Schema** (V7, V8 migrations)
   - Run migrations first to update schema

2. **Domain Models** (EnhancementType, EnhancementRecord, FundingSourceCandidate)
   - Update models to match new schema

3. **Repositories** (add missing query methods)
   - Add findByStatus(), findByCandidateId(), etc.

4. **Services** (update signatures and implementation)
   - Change approveCandidate/rejectCandidate signatures
   - Update implementation to set new fields

5. **Unit Tests** (update existing tests)
   - Fix CandidateValidationServiceTest to use new signatures

6. **Integration Tests** (run to verify)
   - All 4 integration tests should now compile and pass

---

## 7. Risk Assessment

### Breaking Changes
- ✅ **BREAKING:** `EnhancementType` enum values completely changed
- ✅ **BREAKING:** Service method signatures changed (2 parameters → 3 parameters)
- ✅ **BREAKING:** EnhancementRecord field names changed (oldValue/newValue → originalValue/suggestedValue)

### Migration Risk
- ⚠️ **MEDIUM:** If enhancement_record table has data, need data migration strategy
- ⚠️ **LOW:** If funding_source_candidate table has approved/rejected records, they'll lose who/when info

### Testing Impact
- ✅ Unit tests need updates (CandidateValidationServiceTest)
- ✅ Contract tests may need updates (if they call approve/reject)
- ✅ Repository integration tests should still pass (no changes to those tests)

---

## 8. Estimated Effort

| Task | Files | Lines Changed | Time Estimate |
|------|-------|---------------|---------------|
| Database migrations | 2 new | ~100 lines | 30 minutes |
| Domain models | 3 files | ~50 lines | 20 minutes |
| Repositories | 2 files | ~20 lines | 10 minutes |
| Services | 2 files | ~30 lines | 20 minutes |
| Unit test updates | 1 file | ~20 lines | 15 minutes |
| Testing & debugging | - | - | 30 minutes |
| **TOTAL** | **10 files** | **~220 lines** | **~2 hours** |

---

## 9. Alternative: Simplify Integration Tests

If the above changes are too invasive, we could **simplify the integration tests** to match current implementation:

- Remove AI tracking tests (AIEnhancementIntegrationTest)
- Remove approval/rejection user tracking
- Focus on basic workflow: discovery → candidate → approval

**Pros:** No breaking changes, faster to implement
**Cons:** Doesn't test AI collaboration (constitutional requirement)

---

## 10. Recommendation

**Implement the full TDD requirements** because:

1. ✅ **Constitutional Requirement:** AI collaboration tracking is mandatory per constitution
2. ✅ **Audit Trail:** Tracking WHO approved/rejected is essential for compliance
3. ✅ **Future-Proof:** AI enhancement tracking will be needed when LM Studio integration is added
4. ✅ **Reasonable Scope:** ~220 lines of code, ~2 hours of work
5. ✅ **Proper TDD:** Tests define requirements, implementation follows

The integration tests are correctly defining what the system **should do**, not what it currently does. This is the essence of TDD.
