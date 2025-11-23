# Research Decisions - Feature 014: Automated Search Adapter Infrastructure

**Feature**: 014-create-automated-crawler
**Date**: 2025-11-17
**Status**: Complete

## Research Questions & Decisions

### R1: HTTP Client Selection

**Question**: Which HTTP client should we use for search adapter implementations?

**Options Considered**:
1. **RestTemplate** (Spring's synchronous HTTP client)
   - Pros: Simple, well-known
   - Cons: Blocking, deprecated in favor of WebClient
2. **Apache HttpClient** (Third-party)
   - Pros: Mature, feature-rich
   - Cons: More complex, blocking
3. **Spring WebClient** (Reactive HTTP client)
   - Pros: Non-blocking, reactive, Spring integration
   - Cons: More complex than RestTemplate

**Decision**: **Spring WebClient**

**Rationale**:
- Non-blocking I/O allows efficient handling of multiple concurrent requests
- Integrates seamlessly with Spring Boot
- Future-proof (RestTemplate is maintenance mode)
- Supports both synchronous and asynchronous patterns
- Better performance for multiple parallel API calls

**Implementation Notes**:
```java
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder()
        .defaultHeader(HttpHeaders.USER_AGENT, "NorthStar-Funding-Discovery/1.0")
        .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        }));
}
```

### R2: Concurrency Pattern

**Question**: How should we handle parallel search execution across multiple engines?

**Options Considered**:
1. **Sequential Execution**
   - Pros: Simple, predictable
   - Cons: Slow (4 engines × 5 seconds = 20+ seconds)
2. **Traditional Thread Pool**
   - Pros: Familiar pattern
   - Cons: Thread overhead, complex configuration
3. **Virtual Threads** (Java 21+)
   - Pros: Lightweight, simple, scales to thousands of concurrent operations
   - Cons: Requires Java 21+ (we have Java 25)

**Decision**: **Java 25 Virtual Threads with ExecutorService**

**Rationale**:
- Virtual threads are extremely lightweight (can create thousands)
- No complex thread pool configuration needed
- Perfect fit for I/O-bound operations (API calls)
- Much faster than sequential (4 engines in parallel vs sequential)
- Simpler code than traditional thread pools

**Implementation Pattern**:
```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
try {
    List<CompletableFuture<List<SearchResult>>> futures = adapters.stream()
        .map(adapter -> CompletableFuture.supplyAsync(() ->
            adapter.search(query, maxResults), executor))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
} finally {
    executor.close();
}
```

### R3: 7-Day Category Distribution Strategy

**Question**: How should we distribute 30 FundingSearchCategory values across 7 days?

**Analysis**:
- Total categories: 30
- Target: Roughly even distribution across Monday-Sunday
- Goal: All categories searched weekly

**Decision**: **Static Distribution with Theme Grouping**

**Distribution** (30 categories → 7 days):
- **Monday** (4): Individual Support
  - INDIVIDUAL_SCHOLARSHIPS, STUDENT_FINANCIAL_AID, TEACHER_SCHOLARSHIPS, ACADEMIC_FELLOWSHIPS
- **Tuesday** (5): Program Support
  - PROGRAM_GRANTS, CURRICULUM_DEVELOPMENT, AFTER_SCHOOL_PROGRAMS, SUMMER_PROGRAMS, EXTRACURRICULAR_ACTIVITIES
- **Wednesday** (3): Infrastructure & Facilities
  - INFRASTRUCTURE_FUNDING, TECHNOLOGY_EQUIPMENT, LIBRARY_RESOURCES
- **Thursday** (3): Teacher & Staff Development
  - TEACHER_DEVELOPMENT, PROFESSIONAL_TRAINING, ADMINISTRATIVE_CAPACITY
- **Friday** (4): STEM & Special Focus
  - STEM_EDUCATION, ARTS_EDUCATION, SPECIAL_NEEDS_EDUCATION, LANGUAGE_PROGRAMS
- **Saturday** (3): Community & Partnerships
  - COMMUNITY_PARTNERSHIPS, PARENT_ENGAGEMENT, NGO_EDUCATION_PROJECTS
- **Sunday** (8): Research & Modern Education (largest group for weekend processing)
  - EDUCATION_RESEARCH, PILOT_PROGRAMS, INNOVATION_GRANTS, EARLY_CHILDHOOD_EDUCATION, ADULT_EDUCATION, VOCATIONAL_TRAINING, EDUCATIONAL_TECHNOLOGY, ARTS_CULTURE

**Rationale**:
- Groups related categories together (easier to analyze effectiveness)
- Distributes workload: weekdays 3-5 categories, weekend 3-8
- Sunday gets largest group (more processing time on weekends)
- Simple static enum-based implementation (no database configuration needed)

**Implementation**:
```java
public enum DayOfWeek {
    MONDAY(List.of(INDIVIDUAL_SCHOLARSHIPS, STUDENT_FINANCIAL_AID, TEACHER_SCHOLARSHIPS, ACADEMIC_FELLOWSHIPS)),
    TUESDAY(List.of(PROGRAM_GRANTS, CURRICULUM_DEVELOPMENT, AFTER_SCHOOL_PROGRAMS, SUMMER_PROGRAMS, EXTRACURRICULAR_ACTIVITIES)),
    // ... etc
}
```

### R4: Adapter Distribution Per Night

**Question**: How should we distribute 4 search adapters across nightly categories?

**Requirement**: All 4 adapters (Brave, Serper, SearXNG, Tavily) must be used EVERY night

**Options Considered**:
1. **One adapter per category** - Not viable (some nights have 3 categories, need 4 adapters)
2. **Round-robin by category** - Simple, fair distribution
3. **Performance-based routing** - Complex, needs historical data

**Decision**: **Round-robin Distribution with Multiple Adapters per Category**

**Strategy**:
- Each category searched with MULTIPLE adapters (not just one)
- Distribution: `adapter_index = category_index % 4`
- Example for Monday (4 categories):
  - Category 0 (INDIVIDUAL_SCHOLARSHIPS): Brave, SearXNG
  - Category 1 (STUDENT_FINANCIAL_AID): Serper, Tavily
  - Category 2 (TEACHER_SCHOLARSHIPS): Brave, SearXNG
  - Category 3 (ACADEMIC_FELLOWSHIPS): Serper, Tavily

**Rationale**:
- Ensures all 4 adapters used every night (requirement satisfied)
- Provides comparison data (same category searched by multiple engines)
- Simple deterministic algorithm
- Builds effectiveness metrics for future optimization

**Implementation**:
```java
for (int i = 0; i < categories.size(); i++) {
    FundingSearchCategory category = categories.get(i);

    // Assign 2 adapters per category using round-robin
    SearchAdapter adapter1 = adapters.get(i % 4);
    SearchAdapter adapter2 = adapters.get((i + 2) % 4);

    // Execute searches with both adapters
}
```

### R5: Zero-Result Handling Strategy

**Question**: How should we handle search queries that return zero results?

**Critical User Requirement**: "If a particular search produces no results, that is not an error. It must however be known that the search query and the search adapter have failed on the given search"

**Decision**: **Track as Effectiveness Metric, NOT Error**

**Approach**:
1. Zero results is a valid outcome (not thrown as exception)
2. Track in `search_session_statistics` table:
   - query_text
   - search_adapter
   - zero_result: true
3. Use for adapter effectiveness analysis
4. Log at INFO level (not WARN or ERROR)

**Rationale**:
- Some categories may genuinely have few funding sources
- Some adapters may not index certain types of content
- Helps identify which adapters work best for which categories
- Prevents alert fatigue from "errors" that aren't actually problems

**Implementation**:
```java
List<SearchResult> results = adapter.search(query, maxResults);

if (results.isEmpty()) {
    logger.info("Zero results for query='{}' adapter={}", query, adapter.getEngineType());
    statistics.recordZeroResult(query, adapter.getEngineType());
    // Continue processing - not an error
} else {
    logger.info("Found {} results for query='{}' adapter={}",
        results.size(), query, adapter.getEngineType());
}
```

### R6: BigDecimal Precision for Confidence Scores

**Question**: How should we represent confidence scores to avoid precision errors?

**Constitutional Requirement** (Principle XII):
> ALL confidence scores MUST use `BigDecimal` with scale 2, NEVER `Double` or `double`

**Decision**: **BigDecimal with Scale 2 (Mandatory)**

**Problem Avoided**:
```java
// WRONG - Floating point precision errors
double confidence = 0.6;
if (confidence >= 0.6) { ... }  // May fail due to 0.5999999999999999

// CORRECT - Exact decimal arithmetic
BigDecimal confidence = new BigDecimal("0.60").setScale(2, RoundingMode.HALF_UP);
if (confidence.compareTo(THRESHOLD) >= 0) { ... }  // Always correct
```

**Implementation Rules**:
1. Always use string constructor: `new BigDecimal("0.85")`
2. Always set scale: `.setScale(2, RoundingMode.HALF_UP)`
3. Database type: `DECIMAL(3,2)` with CHECK constraint
4. Comparisons: `.compareTo()` not `==`
5. Arithmetic: `.add()`, `.multiply()`, etc.

**Database Migration Pattern**:
```sql
ALTER TABLE funding_source_candidate
  ALTER COLUMN confidence_score TYPE DECIMAL(3,2),
  ADD CONSTRAINT confidence_score_range
    CHECK (confidence_score >= 0.00 AND confidence_score <= 1.00);
```

---

## Research Summary

All 6 research questions answered with clear decisions:
- ✅ R1: Spring WebClient for HTTP
- ✅ R2: Virtual Threads for concurrency
- ✅ R3: Static 7-day category distribution with theme grouping
- ✅ R4: Round-robin adapter distribution (all 4 used nightly)
- ✅ R5: Zero results tracked as effectiveness metrics (not errors)
- ✅ R6: BigDecimal scale 2 for confidence scores (constitutional)

**Next Phase**: Data model design and contract definitions
