# REST API Implementation Progress

## Status: PARTIALLY COMPLETE (T025-T027)

### ✅ Completed

**T025: SearchController Test (RED phase)**
- Created `northstar-rest-api` module with clean dependencies
- 4 comprehensive integration tests:
  - `executeSearch_WithValidRequest_ReturnsInitiatedStatus`
  - `executeSearch_PublishesSearchRequestEventsToKafka`
  - `executeSearch_WithMissingRequiredFields_Returns400BadRequest`
  - `executeSearch_WithInvalidMaxResults_Returns400BadRequest`
- Tests compile and fail as expected (404 NOT_FOUND)
- Uses embedded Kafka for real event publication testing

**T027: SpringDoc OpenAPI Configuration**
- `OpenAPIConfig.java` created with full API documentation
- Swagger UI will be available at `http://localhost:8090/swagger-ui.html`
- OpenAPI JSON at `http://localhost:8090/v3/api-docs`

**Module Structure:**
```
northstar-rest-api/
├── DTOs: SearchExecutionRequest, SearchExecutionResponse
├── Controller: SearchController (90% complete)
├── Config: OpenAPIConfig ✅
├── Tests: SearchControllerTest ✅
└── Dependencies: Minimal (kafka-common, query-generation, domain, springdoc)
```

### ⚠️ BLOCKER: API Mismatch (T026)

**Problem**: REST API DTO doesn't match QueryGenerationService interface

**REST API Request** (`SearchExecutionRequest`):
```java
record SearchExecutionRequest(
    Set<FundingSourceType> fundingSourceTypes,
    Set<FundingMechanism> fundingMechanisms,
    ProjectScale projectScale,
    Set<BeneficiaryPopulation> beneficiaryPopulations,
    Set<RecipientOrganizationType> recipientOrganizationTypes,
    Set<String> geographicScope,
    QueryLanguage queryLanguage,
    int maxResultsPerQuery
)
```

**QueryGenerationService Expects**:
```java
CompletableFuture<QueryGenerationResponse> generateQueries(
    QueryGenerationRequest request  // Single complex object
)

// Where QueryGenerationRequest has:
class QueryGenerationRequest {
    SearchEngineType searchEngine;           // ❌ Not in REST request!
    Set<FundingSearchCategory> categories;   // ❌ Different structure!
    GeographicScope geographic;              // ❌ Enum, not Set<String>!
    int maxQueries;
    UUID sessionId;
    // ... Feature 005 enhanced taxonomy fields
}
```

**Solutions**:

**Option A: Adapter Method in Controller** (Recommended)
Create a private method to map REST DTO → QueryGenerationRequest:
```java
private QueryGenerationRequest buildQueryRequest(
    SearchExecutionRequest request,
    SearchEngineType engine,
    UUID sessionId
) {
    return QueryGenerationRequest.builder()
        .searchEngine(engine)
        .categories(mapToCategories(request))  // Convert enums
        .geographic(mapToGeographic(request))  // String → enum
        .maxQueries(3)  // Fixed for now
        .sessionId(sessionId)
        .fundingSourceTypes(request.fundingSourceTypes())
        .fundingMechanisms(request.fundingMechanisms())
        .projectScale(request.projectScale())
        // ... map other fields
        .build();
}
```

**Option B: Simplify QueryGenerationService**
Add overloaded method that accepts individual parameters (breaking change)

**Option C: Change REST DTO**
Make REST API match QueryGenerationService (worse UX)

### What I Did

1. ✅ Created `northstar-rest-api` module following your architecture vision:
   - REST/CLI/Scheduler will all be entry points publishing to Kafka
   - Workflow engine (search-workflow) processes events
   - Clean separation of concerns

2. ✅ Added module to parent POM

3. ✅ Created DTOs with validation annotations

4. ✅ Created OpenAPI configuration

5. ✅ Created comprehensive integration tests (TDD RED phase)

6. ⚠️ **Partially** created SearchController - needs adapter method to bridge API mismatch

### Next Steps

1. Implement adapter method in SearchController (Option A)
2. Run tests to verify GREEN phase
3. Consider refactoring QueryGenerationService API in future for better ergonomics
4. Document API mismatch as technical debt

### Files Created

**Production Code:**
- `northstar-rest-api/pom.xml`
- `src/main/java/com/northstar/funding/rest/dto/SearchExecutionRequest.java`
- `src/main/java/com/northstar/funding/rest/dto/SearchExecutionResponse.java`
- `src/main/java/com/northstar/funding/rest/config/OpenAPIConfig.java`
- `src/main/java/com/northstar/funding/rest/controller/SearchController.java` (90% done)

**Test Code:**
- `src/test/java/com/northstar/funding/rest/TestApplication.java`
- `src/test/java/com/northstar/funding/rest/controller/SearchControllerTest.java`
- `src/test/resources/application-test.yml`

### Architecture Decision: northstar-rest-api Module

**Why separate module?**
- CLI and Scheduler modules will also trigger workflows
- All three publish to Kafka (single responsibility)
- REST API can run standalone (microservice-ready)
- Minimal dependencies (not a "god module")

**Dependency Graph:**
```
northstar-rest-api
  ├─> northstar-kafka-common (publish events)
  ├─> northstar-query-generation (generate queries)
  ├─> northstar-domain (DTOs, enums)
  └─> northstar-persistence (session management)

northstar-application (future)
  └─> northstar-rest-api (embed REST API)
```

This aligns perfectly with your stated vision for CLI and Scheduler modules.

---

**User Action Required**: Choose solution for API mismatch (recommend Option A) and I'll complete implementation.
