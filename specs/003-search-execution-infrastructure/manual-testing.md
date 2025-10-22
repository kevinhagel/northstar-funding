# Manual Testing Guide - Search Execution Infrastructure (Feature 003)

**Last Updated**: 2025-10-21
**Feature**: Search Execution Infrastructure
**Status**: Ready for Manual Testing

## Prerequisites

### 1. Infrastructure Setup (Mac Studio @ 192.168.1.10)

#### PostgreSQL Database
```bash
# Verify PostgreSQL is running
ssh macstudio "pg_isready -h localhost -p 5432"

# Connect to database
ssh macstudio "psql -U northstar_user -d northstar_funding"

# Verify schema
\dt
# Should show: search_queries, search_session_statistics, discovery_sessions, etc.
```

#### Searxng (Self-hosted Search Engine)
```bash
# Verify Searxng is accessible
curl -s http://192.168.1.10:8080/search?q=test&format=json | jq '.results | length'

# Expected: Returns number of results (usually 10-25)
```

#### LM Studio (Local AI - Optional for this feature)
```bash
# Verify LM Studio API is running
ssh macstudio "curl -s http://localhost:1234/v1/models || echo 'LM Studio not responding at port 1234'"

# Expected: Returns model list or error message
```

### 2. API Keys Configuration

Create `.env` file in `backend/` directory:

```bash
# Tavily AI Search API
TAVILY_API_KEY=tvly-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Perplexity AI Search API
PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Optional: Browserbase (not required for MVP)
BROWSERBASE_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**To obtain API keys**:
- Tavily: https://tavily.com/ (Free tier: 1000 requests/month)
- Perplexity: https://www.perplexity.ai/api (Free tier: 100 requests/month)

### 3. Application Configuration

Verify `backend/src/main/resources/application.yml`:

```yaml
search:
  engines:
    searxng:
      enabled: true
      base-url: http://192.168.1.10:8080

    tavily:
      enabled: true
      api-key: ${TAVILY_API_KEY:}

    perplexity:
      enabled: true
      api-key: ${PERPLEXITY_API_KEY:}

discovery:
  schedule:
    enabled: ${DISCOVERY_SCHEDULE_ENABLED:false}  # Disable for manual testing
```

---

## Test Scenario 1: Individual Adapter Testing

### Step 1.1: Test Searxng Adapter

```bash
# From backend directory
cd backend

# Run Searxng adapter test
mvn test -Dtest=SearxngAdapterTest

# Expected: Tests pass (if Searxng is accessible at 192.168.1.10:8080)
```

**Manual verification**:
```bash
# Direct HTTP test
curl "http://192.168.1.10:8080/search?q=bulgaria+grants&format=json" | jq '.results[0:3]'

# Expected: JSON array with search results containing url, title, content
```

### Step 1.2: Test Tavily Adapter

```bash
# Ensure TAVILY_API_KEY is set
export TAVILY_API_KEY=tvly-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Run Tavily adapter test
mvn test -Dtest=TavilyAdapterTest

# Expected: Tests pass (if API key is valid)
```

**Manual verification**:
```bash
# Direct API test
curl -X POST https://api.tavily.com/search \
  -H "Authorization: Bearer $TAVILY_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"query": "bulgaria grants", "max_results": 5}' | jq '.results | length'

# Expected: Returns 5 (or fewer if fewer results available)
```

### Step 1.3: Test Perplexity Adapter

```bash
# Ensure PERPLEXITY_API_KEY is set
export PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Run Perplexity adapter test
mvn test -Dtest=PerplexityAdapterTest

# Expected: Tests pass (if API key is valid)
```

**Manual verification**:
```bash
# Direct API test
curl -X POST https://api.perplexity.ai/chat/completions \
  -H "Authorization: Bearer $PERPLEXITY_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.1-sonar-small-128k-online",
    "messages": [{"role": "user", "content": "Find bulgaria education grants 2025"}]
  }' | jq '.citations | length'

# Expected: Returns number of citations (usually 5-10)
```

---

## Test Scenario 2: Search Execution Service

### Step 2.1: Test Single Query Execution

```bash
# Run search execution integration test
mvn test -Dtest=SearchExecutionServiceTest

# Expected: Tests pass, shows results from multiple engines
```

### Step 2.2: Verify Domain Deduplication

```bash
# Run domain deduplication test
mvn test -Dtest=DomainDeduplicationTest

# Expected output:
# - Total results: 50-75 (3 engines × 25 results, some may fail)
# - Unique domains: 20-40 (after deduplication)
# - Deduplication rate: 40-60%
```

### Step 2.3: Check Logs for Deduplication Events

```bash
# Run with debug logging
mvn test -Dtest=DomainDeduplicationTest | grep "Duplicate domain"

# Expected output examples:
# DEBUG - Duplicate domain found: example.org (from TAVILY)
# DEBUG - Duplicate domain found: grants.gov (from PERPLEXITY)
```

---

## Test Scenario 3: Circuit Breaker Behavior

### Step 3.1: Test Circuit Breaker with Invalid API Key

```bash
# Configure invalid Tavily API key to trigger failures
export TAVILY_API_KEY=INVALID_KEY_FOR_TESTING

# Run circuit breaker test
mvn test -Dtest=CircuitBreakerTest

# Expected output:
# - Circuit breaker opens after 5 failures (or 3 with test config)
# - Other engines (Perplexity, Searxng) continue working
# - System returns degraded results (fewer total results)
```

### Step 3.2: Monitor Circuit Breaker State

```bash
# Run application with actuator endpoints
mvn spring-boot:run

# In another terminal, check health endpoint
curl http://localhost:8080/api/actuator/health | jq '.components.circuitBreakers'

# Expected: Shows circuit breaker states (CLOSED, OPEN, HALF_OPEN) for each engine
```

---

## Test Scenario 4: Monday Nightly Discovery Session

### Step 4.1: Verify Monday Query Configuration

```bash
# Connect to database
ssh macstudio "psql -U northstar_user -d northstar_funding"

# Query Monday queries
SELECT id, query_text, day_of_week, enabled, tags, target_engines
FROM search_queries
WHERE day_of_week = 'MONDAY' AND enabled = true;

# Expected: 5 queries configured for Monday
```

### Step 4.2: Run Monday Discovery Integration Test

```bash
# Run Monday nightly discovery test
mvn test -Dtest=MondayNightlyDiscoveryTest -DfailIfNoTests=false

# Expected output:
# - Loads 5 Monday queries
# - All queries target 3 engines (SEARXNG, TAVILY, PERPLEXITY)
# - Tests pass
```

### Step 4.3: Manually Trigger Nightly Discovery

```bash
# Start application
mvn spring-boot:run

# In another terminal, trigger scheduler manually (via JMX or custom endpoint)
# Note: This requires implementing a manual trigger endpoint or using JMX

# Alternative: Run integration test which simulates the process
mvn test -Dtest=MondayNightlyDiscoveryTest
```

### Step 4.4: Verify Discovery Session Data

```bash
# Connect to database
ssh macstudio "psql -U northstar_user -d northstar_funding"

# Check discovery sessions (if created)
SELECT id, session_type, start_time, total_search_queries_executed, search_query_set_name
FROM discovery_sessions
ORDER BY start_time DESC
LIMIT 5;

# Check search session statistics
SELECT session_id, engine_type, queries_executed, results_returned, avg_response_time_ms, failure_count
FROM search_session_statistics
ORDER BY created_at DESC
LIMIT 10;
```

---

## Test Scenario 5: Performance Testing

### Step 5.1: Test Single Query Performance

```bash
# Run performance test for single query
mvn test -Dtest=SearchPerformanceTest#shouldExecuteSingleQueryInUnder15Seconds

# Expected output:
# === Single Query Performance ===
# Duration: 3000-8000ms (3-8 seconds)
# Total Results: 50-75
# Results by Engine:
#   - SEARXNG: 0-25
#   - TAVILY: 20-25
#   - PERPLEXITY: 5-10
```

### Step 5.2: Test 10 Queries Performance

**WARNING**: This test makes 30 API calls (10 queries × 3 engines) and may take 5-15 minutes.

```bash
# Run performance test for 10 queries
mvn test -Dtest=SearchPerformanceTest#shouldExecute10QueriesInUnder30Minutes

# Expected output:
# === 10 Queries Performance ===
# Total Queries: 10
# Duration: 300000-900000ms (5-15 minutes)
# Total Results: 500-750
# Average Results per Query: 50-75
```

### Step 5.3: Analyze Response Time Percentiles

```bash
# Run percentile analysis test
mvn test -Dtest=SearchPerformanceTest#shouldMeasureResponseTimePercentiles

# Expected output:
# === Response Time Percentiles ===
# Statistics:
#   Min: 2500ms
#   p50 (median): 4500ms
#   p90: 7000ms
#   p95: 8500ms
#   Max: 10000ms
#   Average: 5200ms
```

---

## Test Scenario 6: End-to-End Workflow

### Step 6.1: Full Integration Test

```bash
# Run all integration tests
mvn test -Dtest=*IntegrationTest,*Test -DfailIfNoTests=false

# Expected: All tests pass
# - Unit tests: Domain, repositories, scheduler
# - Integration tests: Monday discovery, deduplication, circuit breaker
# - Performance tests: Single query, 10 queries, percentiles
```

### Step 6.2: Review Test Coverage

```bash
# Generate test coverage report (if JaCoCo configured)
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html

# Target coverage:
# - Domain layer: >90%
# - Application layer: >80%
# - Infrastructure layer: >70%
```

---

## Troubleshooting Guide

### Issue: Searxng Connection Refused

**Symptoms**:
```
I/O error on GET request for "http://192.168.1.10:8080/search": Connection refused
```

**Solutions**:
1. Verify Searxng is running on Mac Studio:
   ```bash
   ssh macstudio "docker ps | grep searxng"
   ```

2. Test direct access:
   ```bash
   curl http://192.168.1.10:8080/search?q=test
   ```

3. Check firewall:
   ```bash
   ssh macstudio "sudo pfctl -sr | grep 8080"
   ```

### Issue: Tavily 401 Unauthorized

**Symptoms**:
```
401 Unauthorized: "Unauthorized: missing or invalid API key"
```

**Solutions**:
1. Verify API key is set:
   ```bash
   echo $TAVILY_API_KEY
   ```

2. Test API key directly:
   ```bash
   curl -X POST https://api.tavily.com/search \
     -H "Authorization: Bearer $TAVILY_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"query": "test", "max_results": 1}'
   ```

3. Check API quota at https://tavily.com/dashboard

### Issue: Perplexity 429 Rate Limit

**Symptoms**:
```
429 Too Many Requests: Rate limit exceeded
```

**Solutions**:
1. Wait for rate limit reset (check `X-RateLimit-Reset` header)
2. Reduce test frequency
3. Upgrade to paid plan at https://www.perplexity.ai/api

### Issue: Circuit Breaker Stuck OPEN

**Symptoms**:
```
Tavily unavailable (circuit breaker OPEN)
```

**Solutions**:
1. Wait for `waitDurationInOpenState` (30 seconds by default)
2. Restart application to reset circuit breaker:
   ```bash
   mvn spring-boot:stop
   mvn spring-boot:start
   ```

3. Check actuator health endpoint:
   ```bash
   curl http://localhost:8080/api/actuator/health
   ```

### Issue: Tests Timing Out

**Symptoms**:
```
Test timed out after 120000 milliseconds
```

**Solutions**:
1. Increase Maven Surefire timeout in `pom.xml`:
   ```xml
   <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
   ```

2. Run tests with extended timeout:
   ```bash
   mvn test -Dsurefire.timeout=600
   ```

3. Check network connectivity to Mac Studio and external APIs

---

## Quality Metrics

### Success Criteria

- ✅ All unit tests pass (domain, repositories, scheduler)
- ✅ All integration tests pass (Monday discovery, deduplication, circuit breaker)
- ✅ Single query completes in <15 seconds (parallel Virtual Threads)
- ✅ 10 queries complete in <30 minutes (sequential queries, parallel engines)
- ✅ Circuit breaker opens after threshold failures
- ✅ System continues with degraded coverage when one engine fails
- ✅ Domain deduplication removes 40-60% of duplicate results

### Performance Benchmarks

| Metric | Target | Acceptable | Notes |
|--------|--------|------------|-------|
| Single query (3 engines, parallel) | <10s | <15s | Virtual Threads parallel execution |
| 10 queries (sequential) | <20m | <30m | Network latency dependent |
| Deduplication rate | 50-60% | 40-70% | Varies by query topic |
| Circuit breaker response | <100ms | <500ms | Fast-fail when OPEN |
| Database query time | <50ms | <200ms | PostgreSQL @ Mac Studio |

### Monitoring Checklist

- [ ] Searxng accessible at http://192.168.1.10:8080
- [ ] Tavily API key valid and within quota
- [ ] Perplexity API key valid and within quota
- [ ] PostgreSQL database accessible at 192.168.1.10:5432
- [ ] All Flyway migrations applied successfully
- [ ] Circuit breaker health indicators registered
- [ ] Query library configured with 7-day schedule
- [ ] Logging shows parallel Virtual Thread execution
- [ ] Domain deduplication reducing result count by 40-60%

---

## Next Steps

After manual testing is complete:

1. **Enable Nightly Scheduler**: Set `DISCOVERY_SCHEDULE_ENABLED=true` in production
2. **Configure Real API Keys**: Update production `.env` with valid keys
3. **Monitor Circuit Breakers**: Check actuator health endpoint daily
4. **Review Search Results**: Manually inspect high-confidence candidates
5. **Tune Query Library**: Adjust queries based on result quality
6. **Move to Feature 004**: AI-powered query generation and metadata judging

---

## Contact

For issues or questions:
- Review logs: `backend/logs/application.log`
- Check GitHub issues: https://github.com/northstar-funding/issues
- Consult CLAUDE.md for development guidelines
