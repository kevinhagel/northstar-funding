# Persistence Layer Design: Automated Funding Discovery Workflow

**Created**: 2025-09-24  
**Status**: Critical Architecture Document  
**Context**: TDD Foundation for Domain Model Implementation

## 🚨 ARCHITECTURAL CRISIS IDENTIFIED

**ROOT CAUSE:** Original development flow implemented domain models, repositories, and services without testing persistence layer first, violating fundamental TDD principles.

**CRITICAL IMPACT:**
- Controllers may return corrupted/incomplete domain objects
- Repository queries may fail silently at runtime
- Service layer business logic may fail due to data access issues  
- Integration tests become unreliable without solid persistence foundation
- Production deployment risks data corruption and system instability

## Persistence Layer Design Requirements

### 1. **Spring Data JDBC Architecture**

**Constitutional Requirement:** Use Spring Data JDBC (not JPA/ORM) for simplicity and performance.

**Key Principles:**
- Aggregate-focused persistence (DDD alignment)
- No lazy loading complexity
- Direct SQL control for performance
- Simplified transaction management
- JSON field support for complex data structures

### 2. **Database Schema Foundation**

**Already Implemented:** ✅
- V1: FundingSourceCandidate table with JSON fields
- V2: ContactIntelligence table with PII encryption support
- V3: AdminUser table with performance tracking
- V4: DiscoverySession table with AI metadata
- V5: EnhancementRecord audit table
- V6: Performance indexes

### 3. **Critical Persistence Testing Requirements**

**Missing - Must Implement Before Domain Models:**

#### **Repository Persistence Tests (@DataJdbcTest)**
Test each repository's ability to:
- **Basic CRUD**: Save, find, update, delete operations
- **Custom Queries**: All @Query methods return expected results  
- **Pagination & Sorting**: Spring Data pagination works correctly
- **Relationships**: Foreign key constraints and cascade operations
- **JSON Fields**: Serialization/deserialization of complex data
- **Null Handling**: Optional fields and nullable constraints
- **Database Constraints**: Unique constraints, check constraints
- **Performance**: Index usage and query optimization

#### **Data Integrity Testing**
- **Referential Integrity**: Foreign key relationships maintained
- **Business Rule Constraints**: Database-level validation works
- **Concurrent Access**: Handle multiple simultaneous operations
- **Transaction Boundaries**: Rollback scenarios work correctly

### 4. **Persistence Layer Architecture Pattern**

```
┌─────────────────────┐
│   Controller Layer  │ ← Returns Domain Objects
└─────────┬───────────┘
          │
┌─────────▼───────────┐
│   Service Layer     │ ← Business Logic + Transactions
└─────────┬───────────┘
          │
┌─────────▼───────────┐
│ Repository Layer    │ ← Data Access (Spring Data JDBC)
└─────────┬───────────┘
          │
┌─────────▼───────────┐
│  Database Schema    │ ← PostgreSQL with Constraints
└─────────────────────┘
```

**Testing Must Follow Same Pattern:**
1. ✅ Database Schema Tests (Migration validation)
2. ❌ **Repository Layer Tests** (Missing - Critical)
3. ❌ **Service Layer Tests** (Missing - Critical) 
4. ❌ **Controller Layer Tests** (Partially done - needs service mocking)
5. ✅ Integration Tests (Planned)

### 5. **Repository Design Patterns**

#### **Aggregate Repository Pattern**
Each aggregate root gets one repository:
- `FundingSourceCandidateRepository` (Aggregate Root)
- `ContactIntelligenceRepository` (Entity within Candidate aggregate)
- `AdminUserRepository` (Aggregate Root)
- `DiscoverySessionRepository` (Aggregate Root)
- `EnhancementRecordRepository` (Value Object - audit trail)

#### **Query Method Patterns**
```java
// Primary queries for business workflows
Page<FundingSourceCandidate> findByStatusOrderByConfidenceScoreDesc(CandidateStatus status, Pageable pageable);

// Custom business logic queries  
@Query("SELECT * FROM funding_source_candidates WHERE status = :status AND confidence_score >= :minConfidence")
Page<FundingSourceCandidate> findHighConfidenceCandidates(@Param("status") CandidateStatus status, @Param("minConfidence") Double minConfidence, Pageable pageable);

// Performance-optimized queries
List<FundingSourceCandidate> findByDiscoverySessionId(UUID discoverySessionId);
```

### 6. **JSON Field Persistence**

**Requirements:**
- Store complex data structures as PostgreSQL JSON/JSONB
- Support querying within JSON fields
- Handle serialization/deserialization automatically
- Maintain type safety in Java domain models

**Example Implementation:**
```java
@Table("funding_source_candidates")
public class FundingSourceCandidate {
    @Column("extracted_data")
    private Map<String, Object> extractedData;  // Raw AI-scraped data as key-value pairs
    
    @Column("geographic_eligibility") 
    private List<String> geographicEligibility;  // JSON array
    
    @Column("tags")
    private Set<String> tags;  // JSON array
}
```

### 7. **Performance Optimization**

**Database Indexes:** ✅ Already implemented in V6 migration
- Confidence score DESC for review queue performance
- Discovery date for audit queries
- Foreign key indexes for relationship queries
- Full-text search indexes for contact intelligence

**Query Optimization Requirements:**
- All paginated queries must use indexes
- Complex searches must leverage PostgreSQL full-text search
- JSON field queries must use JSONB operators efficiently
- Relationship queries must avoid N+1 problems

### 8. **Security and Encryption**

**PII Protection (Constitutional Requirement):**
- ContactIntelligence email/phone fields encrypted at application layer
- Encryption handled in service layer, transparent to repositories
- Database stores encrypted values, application handles decryption

**Access Control:**
- Repository queries must support reviewer assignment filtering
- Admin users can only access assigned candidates
- Audit trail must track all data access

## Implementation Priority

### **Phase 1: Repository Persistence Tests (CRITICAL)**
Create comprehensive @DataJdbcTest suite for each repository:
1. FundingSourceCandidateRepositoryTest
2. ContactIntelligenceRepositoryTest  
3. AdminUserRepositoryTest
4. DiscoverySessionRepositoryTest
5. EnhancementRecordRepositoryTest

### **Phase 2: Service Layer Tests (CRITICAL)**
Create business logic tests with mocked repositories:
1. CandidateValidationServiceTest
2. ContactIntelligenceServiceTest
3. DiscoveryOrchestrationServiceTest

### **Phase 3: Controller Integration**
Only after persistence and service layers are tested and working.

## Success Criteria

**✅ Repository Layer:**
- All custom @Query methods return expected results
- Pagination and sorting work correctly across all entities
- JSON fields serialize/deserialize properly
- Database constraints enforce business rules
- Performance meets <500ms requirement

**✅ Service Layer:**
- Business rules validated correctly
- Transactions coordinate multiple repositories
- Error handling translates to appropriate exceptions
- Security enforcement works at service boundaries
- Workflow orchestration handles complex operations

**✅ Integration Ready:**
- Controllers can reliably return complete, correct domain objects
- Full workflow tests can depend on solid foundation
- Production deployment has minimal data corruption risk

---

**NEXT ACTIONS:**
1. Implement repository persistence tests (T012.1-T012.5)
2. Implement service layer business logic tests (T032.1-T032.3)  
3. Only then proceed with controller implementation and integration tests

This persistence layer design ensures controllers can reliably return domain objects without data corruption or incomplete results.
