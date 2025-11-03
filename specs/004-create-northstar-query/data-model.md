# Data Model: AI-Powered Search Query Generation

**Feature**: 004-create-northstar-query
**Date**: 2025-11-02

## Overview

This module introduces new model classes for query generation requests, responses, and caching. It reuses existing domain entities from `northstar-domain` and persistence layer from `northstar-persistence`.

## New Model Classes

### 1. QueryGenerationRequest

**Purpose**: Request object for query generation

**Package**: `com.northstar.funding.querygeneration.model`

**Fields**:
- `SearchProvider provider` - Target search provider (BRAVE_SEARCH, SERPER, SEARXNG, TAVILY)
- `Set<FundingSearchCategory> categories` - Funding categories to target
- `GeographicScope geographic` - Geographic region to target
- `int maxQueries` - Number of queries to generate (1-50)
- `UUID sessionId` - Discovery session identifier

**Validation**:
- `provider` - NOT NULL
- `categories` - NOT NULL, NOT EMPTY
- `geographic` - NOT NULL
- `maxQueries` - Range: 1-50
- `sessionId` - NOT NULL

**Example**:
```java
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(10)
    .sessionId(UUID.randomUUID())
    .build();
```

---

### 2. QueryGenerationResponse

**Purpose**: Response object containing generated queries

**Package**: `com.northstar.funding.querygeneration.model`

**Fields**:
- `SearchProvider provider` - Provider these queries target
- `List<String> queries` - Generated query strings
- `boolean fromCache` - True if retrieved from cache
- `Instant generatedAt` - When queries were generated/retrieved
- `UUID sessionId` - Discovery session identifier

**Invariants**:
- `queries` list is immutable
- `queries` contains 0-maxQueries elements
- If `fromCache=true`, `generatedAt` is retrieval time, not original generation time

**Example**:
```java
QueryGenerationResponse response = QueryGenerationResponse.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .queries(List.of(
        "Bulgaria education infrastructure grants",
        "EU structural funds schools Eastern Europe"
    ))
    .fromCache(false)
    .generatedAt(Instant.now())
    .sessionId(sessionId)
    .build();
```

---

### 3. QueryCacheKey

**Purpose**: Cache key for Caffeine cache

**Package**: `com.northstar.funding.querygeneration.model`

**Fields**:
- `SearchProvider provider`
- `Set<FundingSearchCategory> categories`
- `GeographicScope geographic`
- `int maxQueries`

**Key Properties**:
- Immutable (`@Value` from Lombok)
- Proper `equals()` and `hashCode()` based on all fields
- Sets are compared by content, not reference

**Example**:
```java
QueryCacheKey key = QueryCacheKey.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(10)
    .build();
```

---

### 4. SearchProvider (Enum)

**Purpose**: Enumerate supported search providers

**Package**: `com.northstar.funding.querygeneration.model`

**Values**:
- `BRAVE_SEARCH` - Brave Search (keyword queries)
- `SERPER` - Serper API (keyword queries)
- `SEARXNG` - SearXNG metasearch (keyword queries)
- `TAVILY` - Tavily AI search (AI-optimized queries)

**Methods**:
- `boolean isKeywordBased()` - Returns true for Brave, Serper, SearXNG
- `boolean isAiOptimized()` - Returns true for Tavily

**Example**:
```java
if (provider.isKeywordBased()) {
    strategy = keywordQueryStrategy;
} else {
    strategy = tavilyQueryStrategy;
}
```

---

## Existing Domain Entities (Reused)

### FundingSearchCategory (from northstar-domain)

**Purpose**: 25 funding category types

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSearchCategory.java`

**Usage in this module**:
- Input to query generation
- Mapped to keywords or conceptual descriptions

**Examples**: INDIVIDUAL_SCHOLARSHIPS, INFRASTRUCTURE_FUNDING, TEACHER_DEVELOPMENT

---

### GeographicScope (from northstar-domain)

**Purpose**: Geographic regions for funding

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/GeographicScope.java`

**Usage in this module**:
- Input to query generation
- Mapped to location keywords or contextual descriptions

**Examples**: BULGARIA, EASTERN_EUROPE, EU_MEMBER_STATES

---

### SearchQuery (from northstar-domain)

**Purpose**: Persisted search query entity

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/SearchQuery.java`

**Fields** (assumed from V10, V17 migrations):
- `UUID id` - Primary key
- `String queryText` - The actual query string
- `SearchEngineType searchEngine` - Which provider (will map to SearchProvider)
- `Set<FundingSearchCategory> categories` - Associated categories
- `GeographicScope geographicScope` - Geographic focus
- `UUID sessionId` - Discovery session
- `Instant generatedAt` - When generated
- `Instant usedAt` - When used for search (nullable)
- `Integer candidatesFound` - Results count (nullable, for quality tracking)

**Usage in this module**:
- Async persistence of generated queries
- Future quality tracking (candidatesFound)

---

## Strategy Pattern Interfaces

### QueryGenerationStrategy

**Purpose**: Interface for provider-specific query generation

**Package**: `com.northstar.funding.querygeneration.strategy`

**Methods**:
```java
public interface QueryGenerationStrategy {
    CompletableFuture<List<String>> generateQueries(
        Set<FundingSearchCategory> categories,
        GeographicScope geographic,
        int maxQueries
    );

    SearchProvider getProvider();
    String getQueryType(); // "keyword" or "ai-optimized"
}
```

**Implementations**:
- `KeywordQueryStrategy` - For Brave, Serper, SearXNG
- `TavilyQueryStrategy` - For Tavily

---

## Mapping Classes

### CategoryMapper

**Purpose**: Map funding categories to search terms

**Package**: `com.northstar.funding.querygeneration.template`

**Methods**:
- `String toKeywords(FundingSearchCategory category)` - For keyword queries
- `String toConceptualDescription(FundingSearchCategory category)` - For AI-optimized queries

**Example**:
```java
CategoryMapper mapper = new CategoryMapper();

// Keyword mapping
mapper.toKeywords(INFRASTRUCTURE_FUNDING)
// → "infrastructure grants, facility funding"

// Conceptual mapping
mapper.toConceptualDescription(INFRASTRUCTURE_FUNDING)
// → "educational infrastructure development and facility modernization"
```

---

### GeographicMapper

**Purpose**: Map geographic scopes to location terms

**Package**: `com.northstar.funding.querygeneration.template`

**Methods**:
- `String toKeywords(GeographicScope scope)` - For keyword queries
- `String toConceptualDescription(GeographicScope scope)` - For AI-optimized queries

**Example**:
```java
GeographicMapper mapper = new GeographicMapper();

// Keyword mapping
mapper.toKeywords(BULGARIA)
// → "Bulgaria"

// Conceptual mapping
mapper.toConceptualDescription(BULGARIA)
// → "Bulgaria, with focus on post-transition educational development"
```

---

## Prompt Templates

### PromptTemplates

**Purpose**: LangChain4j prompt templates with variable substitution

**Package**: `com.northstar.funding.querygeneration.template`

**Templates**:
1. `KEYWORD_QUERY_TEMPLATE` - For traditional search engines
2. `TAVILY_QUERY_TEMPLATE` - For AI-powered search

**Variables**:
- `{{categories}}` - Comma-separated category descriptions
- `{{geographic}}` - Geographic description
- `{{maxQueries}}` - Number of queries to generate

**Example Usage**:
```java
Map<String, Object> variables = Map.of(
    "categories", "infrastructure grants, facility funding",
    "geographic", "Bulgaria",
    "maxQueries", 10
);

String prompt = PromptTemplates.KEYWORD_QUERY_TEMPLATE
    .apply(variables)
    .text();
```

---

## Data Flow

### Query Generation Flow

```
1. QueryGenerationRequest created
   ↓
2. Check QueryCacheKey in Caffeine cache
   ↓
3. Cache hit? → Return QueryGenerationResponse (fromCache=true)
   ↓
4. Cache miss → Get QueryGenerationStrategy
   ↓
5. Strategy maps categories → keywords/descriptions (CategoryMapper)
   ↓
6. Strategy maps geographic → location terms (GeographicMapper)
   ↓
7. Strategy builds prompt from template + mappings
   ↓
8. LM Studio generates queries (CompletableFuture)
   ↓
9. Parse response into List<String>
   ↓
10. Cache queries (Caffeine)
    ↓
11. Persist queries asynchronously (SearchQuery entity)
    ↓
12. Return QueryGenerationResponse (fromCache=false)
```

---

## Validation Rules

### Input Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| provider | NOT NULL | "Search provider is required" |
| categories | NOT NULL, NOT EMPTY | "At least one funding category is required" |
| geographic | NOT NULL | "Geographic scope is required" |
| maxQueries | 1 ≤ x ≤ 50 | "Max queries must be between 1 and 50" |
| sessionId | NOT NULL, valid UUID | "Valid session ID is required" |

### Output Validation

| Field | Rule | Handling |
|-------|------|----------|
| queries | NOT NULL | Return empty list on generation failure |
| queries | Length ≤ maxQueries | Truncate if LLM over-generates |
| queries | No empty strings | Filter out empty/whitespace-only queries |
| queries | Length < 500 chars | Truncate or filter excessively long queries |

---

## State Transitions

### Cache Entry Lifecycle

```
[NEW] → [CACHED] → [EXPIRED/EVICTED]
  ↑         ↓
  └─────────┘
    (24hr TTL or LRU)
```

### Search Query Entity Lifecycle

```
[GENERATED] → [USED] → [EVALUATED]
     ↓          ↓           ↓
generatedAt   usedAt   candidatesFound
```

---

## Relationships

```
QueryGenerationRequest
    ├─ contains → SearchProvider (enum)
    ├─ contains → Set<FundingSearchCategory> (domain entity)
    ├─ contains → GeographicScope (domain entity)
    └─ generates → QueryGenerationResponse
        └─ persists to → SearchQuery (domain entity)

QueryCacheKey
    ├─ identifies → Cached query set in Caffeine
    └─ built from → QueryGenerationRequest fields

QueryGenerationStrategy
    ├─ implemented by → KeywordQueryStrategy
    ├─ implemented by → TavilyQueryStrategy
    ├─ uses → CategoryMapper
    ├─ uses → GeographicMapper
    └─ uses → PromptTemplates
```

---

## Summary

**New Classes**: 4 model classes, 1 enum, 1 interface, 2 implementations, 2 mappers, 1 template class = **13 total**

**Reused Entities**: 3 domain entities (FundingSearchCategory, GeographicScope, SearchQuery)

**Dependencies**:
- `northstar-domain` - Domain entities and enums
- `northstar-persistence` - SearchQuery entity and repository

**No database schema changes required** - All persistence uses existing `search_queries` table (V10, V17 migrations)
