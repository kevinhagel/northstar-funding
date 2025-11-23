# Quickstart Guide - Feature 014: Manual Search Execution

**Feature**: 014-create-automated-crawler
**Purpose**: Execute first manual search to validate search adapter infrastructure
**Date**: 2025-11-17

## Goal

Execute a manual search with real queries, real search adapters, and real confidence scoring to populate the database with FundingSourceCandidate records that can be viewed in the Admin Dashboard.

## Prerequisites

### 1. Infrastructure Running

**PostgreSQL** (Mac Studio @ 192.168.1.10:5432):
```bash
# Verify database connection
psql -h 192.168.1.10 -U northstar_user -d northstar_funding -c "SELECT COUNT(*) FROM funding_source_candidate;"
```

**Ollama** (Mac Studio @ 192.168.1.10:11434):
```bash
# Verify Ollama is running and has llama3.1:8b model
curl http://192.168.1.10:11434/v1/models | jq '.data[] | select(.id == "llama3.1:8b")'
```

**SearXNG** (Mac Studio @ 192.168.1.10:8080):
```bash
# Verify SearXNG is accessible
curl -s "http://192.168.1.10:8080/search?q=test&format=json" | jq '.results | length'
```

### 2. API Keys Configured

Create `.env` file in project root (DO NOT commit to git):
```bash
# Required for Brave Search
export BRAVE_API_KEY=your_brave_api_key_here

# Required for Serper.dev
export SERPER_API_KEY=your_serper_api_key_here

# Required for Tavily
export TAVILY_API_KEY=your_tavily_api_key_here

# SearXNG does not require API key (self-hosted)
```

Load environment variables:
```bash
source .env
```

### 3. Build Project

```bash
cd /Users/kevin/github/northstar-funding
mvn clean install
```

## Scenario: First Manual Search

### Step 1: Create ManualSearchRequest

**Test Parameters**:
- Categories: [INDIVIDUAL_SCHOLARSHIPS, STEM_EDUCATION]
- Engines: [TAVILY, SEARXNG]
- Geographic scope: "Bulgaria, Eastern Europe, EU"
- Max results per query: 10

**Expected Workflow**:
```
1. QueryGenerationService generates 3 queries per category
   → 6 total queries (2 categories × 3 queries)

2. Execute searches:
   - Tavily: 6 queries × 10 results = ~60 results
   - SearXNG: 6 queries × 10 results = ~60 results
   → ~120 total search results

3. SearchResultProcessor:
   - Extract domains: ~50-80 unique domains
   - Check blacklist: remove blacklisted
   - Calculate confidence: 0.00-1.00 per result (BigDecimal)
   - Filter threshold: keep >= 0.60
   → Estimate: 20-40 high-confidence candidates

4. Database populated:
   - discovery_session: 1 row
   - search_result: ~120 rows
   - domain: ~50-80 rows
   - funding_source_candidate: ~20-40 rows (PENDING_CRAWL status)
```

### Step 2: Execute via Spring Boot Application

**Option A: Via REST API** (if REST endpoint implemented):
```bash
curl -X POST http://localhost:8080/api/search/execute \
  -H "Content-Type: application/json" \
  -d '{
    "categories": ["INDIVIDUAL_SCHOLARSHIPS", "STEM_EDUCATION"],
    "engines": ["TAVILY", "SEARXNG"],
    "maxResultsPerQuery": 10,
    "geographicScope": "Bulgaria, Eastern Europe, EU"
  }'
```

**Option B: Via Command-Line Runner** (if CLI implemented):
```bash
java -jar northstar-application/target/northstar-application-*.jar \
  --search.manual=true \
  --search.categories=INDIVIDUAL_SCHOLARSHIPS,STEM_EDUCATION \
  --search.engines=TAVILY,SEARXNG \
  --search.maxResults=10
```

**Option C: Via JUnit Integration Test** (for initial validation):
```java
@Test
void executeFirstManualSearch() {
    // Given
    ManualSearchRequest request = ManualSearchRequest.builder()
        .categories(List.of(
            FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
            FundingSearchCategory.STEM_EDUCATION
        ))
        .engines(List.of(
            SearchEngineType.TAVILY,
            SearchEngineType.SEARXNG
        ))
        .maxResultsPerQuery(10)
        .geographicScope("Bulgaria, Eastern Europe, EU")
        .build();

    // When
    SearchWorkflowResult result = searchWorkflowService.executeManualSearch(request);

    // Then
    assertThat(result.getQueriesGenerated()).isEqualTo(6);
    assertThat(result.getTotalResultsFound()).isGreaterThan(0);
    assertThat(result.getCandidatesCreated()).isGreaterThan(0);

    logger.info("Manual search complete: {}", result);
}
```

### Step 3: Verify Results in Database

**Check discovery session**:
```sql
SELECT
    session_id,
    session_type,
    started_at,
    completed_at,
    total_results_found,
    candidates_created,
    duplicates_skipped,
    blacklisted_skipped
FROM discovery_session
ORDER BY started_at DESC
LIMIT 1;
```

**Check search results**:
```sql
SELECT
    search_engine_source,
    COUNT(*) AS result_count
FROM search_result
WHERE session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
GROUP BY search_engine_source;
```

**Check domains registered**:
```sql
SELECT
    domain_name,
    domain_status,
    quality_score
FROM domain
WHERE first_discovered_session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
ORDER BY quality_score DESC
LIMIT 20;
```

**Check candidates created**:
```sql
SELECT
    metadata_title,
    source_url,
    confidence_score,
    status,
    search_engine_source
FROM funding_source_candidate
WHERE session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
ORDER BY confidence_score DESC
LIMIT 20;
```

**Check zero-result tracking**:
```sql
SELECT
    search_engine,
    query_text,
    results_count,
    zero_result
FROM search_session_statistics
WHERE session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
    AND zero_result = true;
```

### Step 4: Verify Results in Admin Dashboard

**Start Admin UI** (if not already running):
```bash
cd /Users/kevin/github/northstar-funding/northstar-admin-ui
npm run dev
# Opens at http://localhost:5173
```

**Start REST API** (if not already running):
```bash
cd /Users/kevin/github/northstar-funding/northstar-rest-api
mvn spring-boot:run
# Opens at http://localhost:8080
```

**View Candidates**:
1. Open http://localhost:5173
2. Should see ~20-40 candidates from search
3. Filter by:
   - Status: PENDING_CRAWL
   - Confidence: >= 0.60
   - Search Engine: TAVILY, SEARXNG
4. Approve a few high-confidence candidates (>= 0.80)
5. Reject a few low-confidence candidates (0.60-0.70)
6. Reload page → verify changes persisted

### Step 5: Analyze Effectiveness

**Which adapter produced more results?**
```sql
SELECT
    ss.search_engine,
    COUNT(*) AS total_searches,
    SUM(ss.results_count) AS total_results,
    SUM(CASE WHEN ss.zero_result THEN 1 ELSE 0 END) AS zero_result_count,
    ROUND(AVG(ss.results_count), 2) AS avg_results_per_query
FROM search_session_statistics ss
WHERE ss.session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
GROUP BY ss.search_engine
ORDER BY total_results DESC;
```

**Which categories produced high-confidence candidates?**
```sql
-- Note: This requires tracking category in funding_source_candidate (future enhancement)
-- For now, infer from query text in search_session_statistics
```

**Confidence score distribution**:
```sql
SELECT
    CASE
        WHEN confidence_score >= 0.90 THEN '0.90-1.00 (Excellent)'
        WHEN confidence_score >= 0.80 THEN '0.80-0.89 (Very Good)'
        WHEN confidence_score >= 0.70 THEN '0.70-0.79 (Good)'
        WHEN confidence_score >= 0.60 THEN '0.60-0.69 (Acceptable)'
        ELSE 'Below Threshold'
    END AS confidence_range,
    COUNT(*) AS candidate_count
FROM funding_source_candidate
WHERE session_id = (SELECT session_id FROM discovery_session ORDER BY started_at DESC LIMIT 1)
GROUP BY confidence_range
ORDER BY confidence_range DESC;
```

## Expected Outcomes

### Success Criteria

✅ **Workflow Completes Successfully**:
- No exceptions thrown during execution
- SearchWorkflowResult returned with statistics
- Execution time < 5 minutes

✅ **Data Populated**:
- 1 discovery_session record created
- ~120 search_result records (2 engines × 6 queries × 10 results)
- ~50-80 domain records (unique domains)
- ~20-40 funding_source_candidate records (>= 0.6 confidence)
- ~0-10 search_session_statistics with zero_result=true

✅ **Quality Metrics**:
- >= 80% of candidates have confidence >= 0.70
- < 5% duplicate domains (deduplication working)
- 0% blacklisted domains in candidates
- Zero results tracked (not treated as errors)

✅ **Admin Dashboard**:
- Candidates visible at http://localhost:5173
- Filtering works (status, confidence, engine)
- Approve/reject actions persist to database

### Common Issues

**Issue**: Ollama query generation timeout
- **Symptom**: QueryGenerationService throws timeout exception
- **Fix**: Increase timeout in application.yml (currently 60s)
- **Check**: `curl http://192.168.1.10:11434/v1/models` - should return llama3.1:8b

**Issue**: Tavily/Brave/Serper API key invalid
- **Symptom**: SearchAdapterException with 401/403 status
- **Fix**: Verify API key in .env file
- **Check**: `echo $TAVILY_API_KEY` should show key value

**Issue**: Zero results from all searches
- **Symptom**: All search_session_statistics have zero_result=true
- **Fix**: Check query generation output - may be too specific
- **Check**: Try broader category like PROGRAM_GRANTS instead of niche category

**Issue**: No high-confidence candidates created
- **Symptom**: Many search results but candidates_created=0
- **Fix**: Check ConfidenceScorer - may need to adjust keyword lists
- **Check**: Query search_result table for URLs - are they funding-related?

**Issue**: Admin UI shows "No candidates found"
- **Symptom**: Database has candidates but UI empty
- **Fix**: Check CORS configuration, verify REST API running on port 8080
- **Check**: `curl http://localhost:8080/api/candidates` should return JSON

## Next Steps After Quickstart

1. **Tune Confidence Scoring** - Adjust keyword lists and TLD weights based on results
2. **Expand to More Categories** - Try Monday schedule (4 categories)
3. **Test All 4 Adapters** - Add Brave and Serper to engines list
4. **Schedule Nightly Execution** - Implement @Scheduled job for automated runs
5. **Analyze Effectiveness** - Review which adapters work best for which categories

## Logging

**Enable debug logging** for detailed workflow tracking:
```yaml
logging:
  level:
    com.northstar.funding.searchadapters: DEBUG
    com.northstar.funding.crawler: DEBUG
    com.northstar.funding.querygeneration: DEBUG
```

**Watch for these log messages**:
- `[SearchWorkflowService] Starting nightly search for day=MONDAY categories=4`
- `[QueryGenerationService] Generated 3 queries for category=INDIVIDUAL_SCHOLARSHIPS`
- `[TavilyAdapter] Searching query='Bulgaria scholarship programs' maxResults=10`
- `[SearchResultProcessor] Processing 120 results from 2 engines`
- `[ConfidenceScorer] Result confidence=0.85 url=https://ec.europa.eu/...`
- `[CandidateCreationService] Created candidate id=... status=PENDING_CRAWL`
- `[SearchWorkflowService] Completed in duration=3m15s candidates=32`

## Manual Execution Script (Future)

Create `scripts/run-manual-search.sh`:
```bash
#!/bin/bash

# Load environment variables
source .env

# Run manual search
java -jar northstar-application/target/northstar-application-*.jar \
  --search.manual=true \
  --search.categories=$1 \
  --search.engines=$2 \
  --search.maxResults=10

# Example usage:
# ./scripts/run-manual-search.sh "INDIVIDUAL_SCHOLARSHIPS,STEM_EDUCATION" "TAVILY,SEARXNG"
```
