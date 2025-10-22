# Data Model: Search Execution Infrastructure

**Feature**: 003-search-execution-infrastructure
**Date**: 2025-10-20
**Phase**: Phase 1 - Design & Contracts

## Overview
This document defines the domain model, DTOs, and database schema for search execution infrastructure. All entities follow DDD principles with clear bounded context separation.

---

## Domain Entities

### 1. SearchQuery

**Purpose**: Represents a search query to be executed across search engines.

**Package**: `com.northstar.funding.discovery.search.domain`

**Attributes**:
```java
public class SearchQuery {
    private Long id;                          // Primary key
    private String queryText;                 // Search query text
    private DayOfWeek dayOfWeek;              // Monday, Tuesday, etc.
    private Set<QueryTag> tags;               // Metadata tags (geography, category, authority)
    private Set<SearchEngineType> targetEngines;  // Which engines to query
    private int expectedResults;              // Expected result count (default: 25)
    private boolean enabled;                  // Active/inactive flag
    private Instant createdAt;                // Creation timestamp
    private Instant updatedAt;                // Last modified timestamp
}
```

**Validation Rules**:
- `queryText`: NOT NULL, length 1-500 characters
- `dayOfWeek`: NOT NULL
- `tags`: NOT NULL, at least 1 tag required
- `targetEngines`: NOT NULL, at least 1 engine required
- `expectedResults`: Must be between 1 and 100
- `enabled`: Defaults to TRUE

**Database Table**: `search_queries`
```sql
CREATE TABLE search_queries (
    id BIGSERIAL PRIMARY KEY,
    query_text VARCHAR(500) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,  -- MONDAY, TUESDAY, etc.
    tags JSONB NOT NULL,                -- [{type: "geography", value: "bulgaria"}, ...]
    target_engines JSONB NOT NULL,      -- ["SEARXNG", "TAVILY", "PERPLEXITY"]
    expected_results INT NOT NULL DEFAULT 25,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_expected_results CHECK (expected_results BETWEEN 1 AND 100)
);

CREATE INDEX idx_search_queries_day_of_week ON search_queries(day_of_week);
CREATE INDEX idx_search_queries_enabled ON search_queries(enabled);
```

---

### 2. QueryTag

**Purpose**: Value object for categorizing search queries.

**Package**: `com.northstar.funding.discovery.search.domain`

**Attributes**:
```java
public record QueryTag(
    TagType type,      // GEOGRAPHY, CATEGORY, AUTHORITY
    String value       // "bulgaria", "grants", "government"
) {
    public QueryTag {
        Objects.requireNonNull(type, "Tag type cannot be null");
        Objects.requireNonNull(value, "Tag value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Tag value cannot be blank");
        }
    }
}

public enum TagType {
    GEOGRAPHY,    // bulgaria, eastern-europe, balkans, eu
    CATEGORY,     // grants, scholarships, fellowships, research
    AUTHORITY     // government, foundation, ngo, corporate
}
```

**JSON Representation**:
```json
{
  "type": "GEOGRAPHY",
  "value": "bulgaria"
}
```

---

### 3. SearchEngineType

**Purpose**: Enum for supported search engines.

**Package**: `com.northstar.funding.discovery.search.domain`

**Values**:
```java
public enum SearchEngineType {
    SEARXNG("Searxng", "http://192.168.1.10:8080", false),
    BROWSERBASE("Browserbase", "https://api.browserbase.com", true),  // DEFERRED
    TAVILY("Tavily", "https://api.tavily.com", true),
    PERPLEXITY("Perplexity", "https://api.perplexity.ai", true);

    private final String displayName;
    private final String defaultBaseUrl;
    private final boolean requiresApiKey;

    SearchEngineType(String displayName, String defaultBaseUrl, boolean requiresApiKey) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.requiresApiKey = requiresApiKey;
    }

    // Getters
}
```

---

### 4. SearchSessionStatistics

**Purpose**: Tracks statistics for a single nightly discovery session.

**Package**: `com.northstar.funding.discovery.search.domain`

**Attributes**:
```java
public class SearchSessionStatistics {
    private Long id;                          // Primary key
    private Long discoverySessionId;          // FK to DiscoverySession
    private Instant startTime;                // Session start timestamp
    private Instant endTime;                  // Session end timestamp
    private int totalQueriesExecuted;         // Number of queries executed
    private int totalResultsCollected;        // Raw results across all engines
    private int uniqueDomainsFound;           // Unique domains after deduplication
    private int highConfidenceCandidates;     // Candidates with confidence >= 0.60
    private int errorsEncountered;            // Total errors across all engines
    private Map<SearchEngineType, EngineStatistics> perEngineStats;  // Per-engine breakdown
    private Map<String, QueryStatistics> perQueryStats;              // Per-query breakdown
}
```

**Nested Types**:
```java
public record EngineStatistics(
    int queriesExecuted,
    int resultsReturned,
    int averageResponseTimeMs,
    int failureCount,
    CircuitBreakerState circuitBreakerState
) {}

public record QueryStatistics(
    String queryText,
    int resultsPerEngine,           // Average results per engine
    int uniqueDomainsDiscovered,
    int highConfidenceHits,         // Results with confidence >= 0.60
    Map<SearchEngineType, Integer> resultsPerEngineBreakdown
) {}

public enum CircuitBreakerState {
    CLOSED,       // Normal operation
    OPEN,         // Circuit breaker tripped
    HALF_OPEN     // Testing recovery
}
```

**Database Table**: `search_session_statistics`
```sql
CREATE TABLE search_session_statistics (
    id BIGSERIAL PRIMARY KEY,
    discovery_session_id BIGINT NOT NULL REFERENCES discovery_sessions(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    total_queries_executed INT NOT NULL DEFAULT 0,
    total_results_collected INT NOT NULL DEFAULT 0,
    unique_domains_found INT NOT NULL DEFAULT 0,
    high_confidence_candidates INT NOT NULL DEFAULT 0,
    errors_encountered INT NOT NULL DEFAULT 0,
    per_engine_stats JSONB NOT NULL DEFAULT '{}',
    per_query_stats JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_session_stats_session_id ON search_session_statistics(discovery_session_id);
CREATE INDEX idx_search_session_stats_start_time ON search_session_statistics(start_time);
```

---

### 5. DiscoverySession (Existing - Extended)

**Purpose**: Tracks a single nightly discovery execution (existing from Feature 002, extended for search).

**Package**: `com.northstar.funding.discovery.domain`

**New Attributes** (added to existing entity):
```java
public class DiscoverySession {
    // ... existing fields ...

    // NEW: Search execution metadata
    private SessionType sessionType;          // MANUAL, SCHEDULED, SEARCH_EXECUTION
    private String searchQuerySetName;        // "Monday", "Tuesday", etc.
    private int totalSearchQueriesExecuted;   // Number of search queries run
}

public enum SessionType {
    MANUAL,            // Manually triggered
    SCHEDULED,         // Nightly scheduler
    SEARCH_EXECUTION,  // Search execution session (NEW)
    DEEP_CRAWL         // Phase 2 web crawling (future)
}
```

**Database Migration**: `V12__extend_discovery_session_for_search.sql`
```sql
-- Add new columns to existing discovery_sessions table
ALTER TABLE discovery_sessions
ADD COLUMN session_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
ADD COLUMN search_query_set_name VARCHAR(50),
ADD COLUMN total_search_queries_executed INT DEFAULT 0;

CREATE INDEX idx_discovery_sessions_session_type ON discovery_sessions(session_type);
```

---

## DTOs (Data Transfer Objects)

### 1. SearchResult (Existing from Feature 002)

**Purpose**: Represents a single search result from any search engine.

**Package**: `com.northstar.funding.discovery.service.dto`

**Attributes** (already defined):
```java
@Builder
public record SearchResult(
    String url,                    // Search result URL
    String title,                  // Result title
    String snippet,                // Result snippet/description
    SearchEngineType source,       // Which engine returned this result
    String originatingQuery,       // Original search query
    int resultPosition,            // Position in search results (1-based)
    Instant timestamp              // When result was retrieved
) {
    // Validation in compact constructor
    public SearchResult {
        Objects.requireNonNull(url, "URL cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(source, "Source cannot be null");
        Objects.requireNonNull(originatingQuery, "Originating query cannot be null");
        if (resultPosition < 1) {
            throw new IllegalArgumentException("Result position must be >= 1");
        }
    }
}
```

**No changes needed** - existing DTO from Feature 002 is sufficient.

---

### 2. SearchEngineRequest

**Purpose**: Request DTO for search engine adapters.

**Package**: `com.northstar.funding.discovery.search.infrastructure.adapters`

**Attributes**:
```java
@Builder
public record SearchEngineRequest(
    String query,              // Search query text
    int maxResults,            // Maximum results to return
    String language,           // Language code (e.g., "en")
    Duration timeout           // Request timeout
) {
    public SearchEngineRequest {
        Objects.requireNonNull(query, "Query cannot be null");
        if (query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be blank");
        }
        if (maxResults < 1 || maxResults > 100) {
            throw new IllegalArgumentException("Max results must be between 1 and 100");
        }
        Objects.requireNonNull(language, "Language cannot be null");
        Objects.requireNonNull(timeout, "Timeout cannot be null");
    }

    public static SearchEngineRequest defaultRequest(String query) {
        return new SearchEngineRequest(query, 25, "en", Duration.ofSeconds(15));
    }
}
```

---

### 3. SearchEngineResponse

**Purpose**: Response DTO from search engine adapters.

**Package**: `com.northstar.funding.discovery.search.infrastructure.adapters`

**Attributes**:
```java
@Builder
public record SearchEngineResponse(
    SearchEngineType engine,         // Which engine provided this response
    List<SearchResult> results,      // Search results
    int totalResults,                // Total available results (may be > results.size)
    Duration responseTime,           // How long the request took
    boolean isSuccess,               // Success/failure indicator
    Optional<String> errorMessage    // Error message if failed
) {
    public SearchEngineResponse {
        Objects.requireNonNull(engine, "Engine cannot be null");
        Objects.requireNonNull(results, "Results cannot be null");
        Objects.requireNonNull(responseTime, "Response time cannot be null");
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
    }

    public static SearchEngineResponse success(
            SearchEngineType engine,
            List<SearchResult> results,
            Duration responseTime) {
        return new SearchEngineResponse(
            engine,
            results,
            results.size(),
            responseTime,
            true,
            Optional.empty()
        );
    }

    public static SearchEngineResponse failure(
            SearchEngineType engine,
            String errorMessage,
            Duration responseTime) {
        return new SearchEngineResponse(
            engine,
            Collections.emptyList(),
            0,
            responseTime,
            false,
            Optional.of(errorMessage)
        );
    }
}
```

---

## Relationships

### Entity Relationship Diagram
```
DiscoverySession (existing)
    ↓ 1:1
SearchSessionStatistics (new)
    ↓ contains
    ├─ per_engine_stats: Map<SearchEngineType, EngineStatistics>
    └─ per_query_stats: Map<String, QueryStatistics>

SearchQuery (new)
    ↓ references
    ├─ DayOfWeek (enum)
    ├─ QueryTag[] (value objects)
    └─ SearchEngineType[] (enum)

SearchResult (existing DTO)
    ↓ passed to
MetadataJudgingService (existing)
    ↓ produces
MetadataJudgment (existing DTO)
    ↓ creates
FundingSourceCandidate (existing)
    ↓ links to
Domain (existing)
```

---

## Database Schema Summary

### New Tables
1. **search_queries**: Stores hardcoded query library (initially loaded from YAML config)
2. **search_session_statistics**: Tracks per-session analytics

### Modified Tables
1. **discovery_sessions**: Add `session_type`, `search_query_set_name`, `total_search_queries_executed`

### Existing Tables (No Changes)
1. **domains**: Domain deduplication (existing from Feature 002)
2. **funding_source_candidates**: Candidate storage (existing from Feature 002)

---

## Configuration Properties

### SearchEngineProperties (existing, from Feature 002)

**Extends existing** `SearchEngineProperties.java`:
```java
@Component
@ConfigurationProperties(prefix = "search")
@Data
public class SearchEngineProperties {
    private Map<String, EngineConfig> engines = new HashMap<>();

    @Data
    public static class EngineConfig {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private int maxResults = 25;
        private int timeoutSeconds = 10;
    }

    // Convenience methods (existing)
    public EngineConfig getSearxng() { return engines.getOrDefault("searxng", new EngineConfig()); }
    public EngineConfig getTavily() { return engines.getOrDefault("tavily", new EngineConfig()); }
    public EngineConfig getPerplexity() { return engines.getOrDefault("perplexity", new EngineConfig()); }
}
```

### DiscoverySchedulingProperties (NEW)

**Package**: `com.northstar.funding.discovery.search.infrastructure.config`

```java
@Component
@ConfigurationProperties(prefix = "discovery.scheduling")
@Data
public class DiscoverySchedulingProperties {
    private boolean enabled = false;            // Disabled by default
    private String cron = "0 0 2 * * *";        // 2:00 AM daily
    private String timezone = "Europe/Sofia";   // Bulgaria timezone
}
```

### SearchQueryLibraryProperties (NEW)

**Package**: `com.northstar.funding.discovery.search.infrastructure.config`

```java
@Component
@ConfigurationProperties(prefix = "discovery.queries")
@Data
public class SearchQueryLibraryProperties {
    private Map<DayOfWeek, List<QueryConfig>> queries = new HashMap<>();

    @Data
    public static class QueryConfig {
        private String query;
        private List<String> tags;              // ["geography:bulgaria", "category:grants"]
        private List<SearchEngineType> engines = List.of(
            SearchEngineType.SEARXNG,
            SearchEngineType.TAVILY,
            SearchEngineType.PERPLEXITY
        );
    }
}
```

---

## Validation & Constraints

### Business Rules
1. **Query Uniqueness**: Same query text should not appear multiple times for same day-of-week
2. **Tag Format**: Tags must follow format `{type}:{value}` (e.g., `geography:bulgaria`)
3. **Engine Availability**: At least 1 search engine must be enabled and configured
4. **Result Limits**: Max 100 results per query per engine (prevent API abuse)
5. **Session Integrity**: SearchSessionStatistics must reference valid DiscoverySession

### Database Constraints
- `CHECK (expected_results BETWEEN 1 AND 100)` on `search_queries.expected_results`
- `FOREIGN KEY (discovery_session_id) REFERENCES discovery_sessions(id)` on `search_session_statistics`
- `NOT NULL` constraints on all required fields

---

## Migration Strategy

### Flyway Migrations

**V10__create_search_queries_table.sql**:
```sql
CREATE TABLE search_queries (
    id BIGSERIAL PRIMARY KEY,
    query_text VARCHAR(500) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    tags JSONB NOT NULL,
    target_engines JSONB NOT NULL,
    expected_results INT NOT NULL DEFAULT 25,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_expected_results CHECK (expected_results BETWEEN 1 AND 100)
);

CREATE INDEX idx_search_queries_day_of_week ON search_queries(day_of_week);
CREATE INDEX idx_search_queries_enabled ON search_queries(enabled);

-- Insert sample queries for Monday
INSERT INTO search_queries (query_text, day_of_week, tags, target_engines)
VALUES
    ('bulgaria funding grants 2025', 'MONDAY',
     '[{"type":"GEOGRAPHY","value":"bulgaria"},{"type":"CATEGORY","value":"grants"}]'::JSONB,
     '["SEARXNG","TAVILY","PERPLEXITY"]'::JSONB),
    ('eastern europe ngo funding', 'MONDAY',
     '[{"type":"GEOGRAPHY","value":"eastern-europe"},{"type":"CATEGORY","value":"ngo"}]'::JSONB,
     '["SEARXNG","TAVILY","PERPLEXITY"]'::JSONB);
```

**V11__create_search_session_statistics_table.sql**:
```sql
CREATE TABLE search_session_statistics (
    id BIGSERIAL PRIMARY KEY,
    discovery_session_id BIGINT NOT NULL REFERENCES discovery_sessions(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    total_queries_executed INT NOT NULL DEFAULT 0,
    total_results_collected INT NOT NULL DEFAULT 0,
    unique_domains_found INT NOT NULL DEFAULT 0,
    high_confidence_candidates INT NOT NULL DEFAULT 0,
    errors_encountered INT NOT NULL DEFAULT 0,
    per_engine_stats JSONB NOT NULL DEFAULT '{}',
    per_query_stats JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_session_stats_session_id ON search_session_statistics(discovery_session_id);
CREATE INDEX idx_search_session_stats_start_time ON search_session_statistics(start_time);
```

**V12__extend_discovery_session_for_search.sql**:
```sql
ALTER TABLE discovery_sessions
ADD COLUMN session_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
ADD COLUMN search_query_set_name VARCHAR(50),
ADD COLUMN total_search_queries_executed INT DEFAULT 0;

CREATE INDEX idx_discovery_sessions_session_type ON discovery_sessions(session_type);
```

---

## Next Steps

1. Create OpenAPI contract for SearchEngineAdapter interface → `contracts/search-engine-api.yaml`
2. Create integration test scenarios → `quickstart.md`
3. Update agent context → `CLAUDE.md`
4. Proceed to Phase 2 task generation

---

*Data model completed: 2025-10-20*
*Ready for contract generation*
