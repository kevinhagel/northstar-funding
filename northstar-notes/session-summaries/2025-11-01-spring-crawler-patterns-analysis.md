# Session Summary: Spring-Crawler Patterns Analysis

**Date**: 2025-11-01
**Session Type**: Research and Analysis
**Status**: Complete - Ready for Search Providers Specification

---

## Executive Summary

Analyzed 4 critical files from the spring-crawler project to extract proven patterns for the northstar-crawler implementation. Spring-crawler was a "failed effort" per user, but contains valuable implementations of query generation, AI validation, and domain deduplication that we can learn from.

**Key Discovery**: Spring-crawler demonstrates working implementations of dual query strategies (keyword vs AI-optimized), multi-model LLM integration, and adaptive domain deduplication that we should adapt for northstar-crawler.

---

## Files Analyzed

### 1. QueryGenerationService.java (676 lines)
**Purpose**: Generate search queries using LM Studio with dual strategies

**Key Patterns**:
- Dual query templates (keyword vs AI-optimized)
- Provider-specific routing (Tavily vs BraveSearch/Serper/SearXNG)
- 25 funding categories in 6 groups
- Comprehensive geographic taxonomy (Bulgaria-first priority matches our needs)
- Dual mapping functions (keywords vs conceptual descriptions)
- LangChain4j LLM integration

### 2. FundingValidationService.java (673 lines)
**Purpose**: AI-powered metadata judging with confidence scoring

**Key Patterns**:
- Multi-model strategy (fast/accurate/complex)
- Adaptive model selection based on complexity
- Virtual threads for parallel LLM processing
- Batched validation with concurrency control
- Resilience4j circuit breaker for LM Studio failures
- Category parsing with normalization

### 3. DomainDeduplicationService.java (307 lines)
**Purpose**: Prevent reprocessing domains using Caffeine cache

**Key Patterns**:
- Three-layer caching (domains/URLs/validations)
- Adaptive cooldown based on result quality
- Domain normalization (www removal, lowercase)
- Vavr functional deduplication
- URL hashing for cache keys
- Cache statistics monitoring

### 4. SearchProvider.java
**Purpose**: Search provider enum

```java
public enum SearchProvider {
    BRAVE_SEARCH, SEARXNG, SERPER, TAVILY, MANUAL_INPUT
}
```

---

## Critical Patterns for NorthStar-Crawler

### 1. Dual Query Strategy

**For Traditional Search** (BraveSearch, Serper, SearXNG):
```
"Bulgaria education infrastructure grants funding opportunities"
```

**For AI-Optimized Search** (Tavily):
```
"Educational infrastructure funding opportunities for modernizing
schools in Bulgaria and Eastern European transition economies"
```

**Why**: Different search engines have different strengths. Traditional engines work best with keyword phrases. Tavily's AI-powered search benefits from natural language context.

### 2. Category Taxonomy (25 Categories in 6 Groups)

Spring-crawler has proven taxonomy we should adopt:

**Infrastructure & Facilities** (4):
- INFRASTRUCTURE_FUNDING, FACILITY_CONSTRUCTION, TECHNOLOGY_INFRASTRUCTURE, RENOVATION_MODERNIZATION

**Individual Support** (4):
- INDIVIDUAL_SCHOLARSHIPS, STUDENT_GRANTS, GRADUATE_FELLOWSHIPS, STUDY_ABROAD_FUNDING

**Educator Development** (4):
- TEACHER_DEVELOPMENT, PROFESSIONAL_DEVELOPMENT, EDUCATOR_GRANTS, LEADERSHIP_TRAINING

**Program Funding** (4):
- PROGRAM_GRANTS, CURRICULUM_DEVELOPMENT, RESEARCH_FUNDING, INNOVATION_GRANTS

**Institutional Support** (4):
- OPERATIONAL_SUPPORT, CAPACITY_BUILDING, ORGANIZATIONAL_DEVELOPMENT, EMERGENCY_FUNDING

**Specialized Funding** (5):
- SPECIAL_NEEDS_EDUCATION, RURAL_EDUCATION, MINORITY_EDUCATION, ARTS_EDUCATION, STEM_EDUCATION

### 3. Geographic Taxonomy (Bulgaria-First Priority)

**European Regions** (PRIORITY):
- BULGARIA (highest)
- EASTERN_EUROPE
- CENTRAL_EUROPE
- WESTERN_EUROPE
- NORTHERN_EUROPE
- SOUTHERN_EUROPE
- BALKANS
- EUROPE

**EU Groupings**:
- EU_MEMBER_STATES
- EU_CANDIDATE_COUNTRIES

**North America** (LOWEST):
- UNITED_STATES (lowest priority per user requirement)
- CANADA

### 4. Multi-Model LLM Strategy

```java
@Qualifier("fastClassificationModel")      // llama3.1:8b - ~4s response
@Qualifier("accurateAnalysisModel")        // phi3:medium - ~6.5s response
@Qualifier("complexReasoningModel")
```

**Adaptive Selection**:
- Simple cases (clear foundation/grant keywords) → Fast model
- Complex cases (ambiguous or long descriptions) → Accurate model

### 5. Virtual Threads for Parallel Processing

```java
CompletableFuture<Try<ValidationResult>> validateCandidateAsync(SearchCandidate candidate) {
    return CompletableFuture.supplyAsync(() -> {
        return validateCandidate(candidate);
    }, llmValidationExecutor);  // Virtual Thread executor
}
```

**Why**: LLM calls are I/O-bound (waiting for LM Studio). Virtual threads allow thousands of concurrent validations without thread pool exhaustion.

### 6. Adaptive Domain Cooldown

```java
private Duration determineCooldownPeriod(DomainCacheEntry entry) {
    if (entry.getValidCandidatesFound() > 0 && entry.getAverageConfidence() > 0.8) {
        return Duration.ofHours(12); // High-quality domains
    } else if (entry.getValidCandidatesFound() > 0) {
        return Duration.ofHours(6);  // Medium-quality
    } else {
        return Duration.ofHours(2);  // Low-quality (retry sooner)
    }
}
```

**Why**: High-quality domains don't need frequent rechecking. Low-quality domains might add content later.

---

## What to Adapt vs Avoid

### ✅ Patterns We Should Use

1. **Dual Query Strategy** - Proven effective for different search engine types
2. **Category Taxonomy** - 25 categories cover educational funding comprehensively
3. **Geographic Taxonomy** - Bulgaria-first priority matches user requirements
4. **Multi-Model LLM** - Fast for simple, accurate for complex
5. **Virtual Threads** - Java 25 supports this natively, perfect for I/O parallelism
6. **Adaptive Cooldown** - Quality-based reprocessing makes sense
7. **Domain Normalization** - Prevents www vs non-www duplicates
8. **Vavr Try Monad** - Clean functional error handling

### ⚠️ Patterns to Simplify

1. **Resilience4j** - May be overkill for v1, add later if needed
2. **Caffeine Cache** - We have PostgreSQL Domain table, may not need separate cache
3. **Spring Cloud Config** - Not needed without distributed config server
4. **Complex Batching** - Start with simpler defaults

### ❌ What Spring-Crawler Got Wrong

User said spring-crawler was a "failed effort":
1. **Over-engineering risk** - May have been too complex
2. **Human-AI hybrid is mandatory** - Automated contact extraction failed (70-80% failure)
3. **Don't confuse phases** - Metadata judging ≠ deep scraping

---

## Implications for Search Providers Spec

### Provider Adapter Pattern

```java
public interface SearchProviderAdapter {
    SearchProviderType getProviderType();
    List<SearchResult> executeSearch(String query, int maxResults);
    boolean supportsKeywordSearch();
    boolean supportsAIOptimizedSearch();
}
```

**Implementations**:
- BraveSearchAdapter (keyword)
- SearxngAdapter (keyword)
- SerperAdapter (keyword)
- TavilyAdapter (AI-optimized)

### Query Generation Integration

Search providers need queries from LM Studio:
- **Keyword queries** → BraveSearch, Serper, SearXNG
- **AI-optimized queries** → Tavily

### Virtual Thread Execution

```java
CompletableFuture<List<SearchResult>> braveResults =
    CompletableFuture.supplyAsync(() -> braveAdapter.executeSearch(query, 20));

CompletableFuture<List<SearchResult>> tavilyResults =
    CompletableFuture.supplyAsync(() -> tavilyAdapter.executeSearch(aiQuery, 10));

List<SearchResult> allResults = CompletableFuture.allOf(braveResults, tavilyResults)
    .thenApply(v -> Stream.concat(
        braveResults.join().stream(),
        tavilyResults.join().stream())
        .collect(Collectors.toList()))
    .join();
```

---

## Technologies Identified

### Core Libraries (from spring-crawler)
- **LangChain4j** - LLM integration framework
- **Vavr** - Functional programming (Try monad, immutable collections)
- **Resilience4j** - Circuit breaker, retry (optional for v1)
- **Caffeine** - High-performance caching (may use PostgreSQL instead)
- **Guava** - Hashing utilities

### Spring Boot Patterns
- Virtual Thread Executor for parallel processing
- @Qualifier for multi-model LLM beans
- @RefreshScope for dynamic configuration (optional)

---

## Next Steps

### Immediate
1. **Create Search Providers Specification** (specs/003b-search-providers/)
   - Define SearchProviderAdapter interface
   - Design four concrete implementations
   - Specify Virtual Thread execution pattern
   - Define result deduplication strategy

### Follow-Up
2. **Document Funding Domain Knowledge** (specs/003a-funding-taxonomy/)
   - Extract 25 categories from spring-crawler
   - Extract geographic taxonomy
   - Add Bulgaria-first priority ordering

3. **Design Weekly Search Schedule**
   - Spread queries across week (don't repeat nightly)
   - Group by geographic priority

4. **Plan Query Generation Integration** (specs/003c-query-generation/)
   - LM Studio adapter with dual templates
   - Fast model for keyword generation
   - Accurate model for AI-optimized generation

---

## Tags

#spring-crawler #patterns-analysis #search-providers #query-generation #ai-validation #domain-deduplication #virtual-threads #langchain4j #vavr #lessons-learned

---

**Session Duration**: ~1 hour (reading 4 files, analyzing patterns)
**Outcome**: Comprehensive pattern analysis complete, ready to create Search Providers spec
**Status**: Ready for `/specify` command
