# Quickstart: Search Provider Adapters

**Feature**: 003-design-and-implement
**Date**: 2025-11-01
**Purpose**: Validate search provider adapters work end-to-end

---

## Prerequisites

Before running this quickstart, ensure:

1. **PostgreSQL Running**: Mac Studio @ 192.168.1.10:5432
   ```bash
   ssh macstudio "docker ps | grep northstar-postgres"
   ```

2. **SearXNG Running**: Mac Studio @ 192.168.1.10:8080
   ```bash
   ssh macstudio "docker ps | grep searxng"
   curl -s http://192.168.1.10:8080/search | head -10
   ```

3. **API Keys Configured**: application-prod.properties exists with valid keys
   ```properties
   search.providers.brave-search.api-key=ACTUAL_BRAVE_KEY
   search.providers.serper.api-key=ACTUAL_SERPER_KEY
   search.providers.tavily.api-key=ACTUAL_TAVILY_KEY
   ```

4. **Database Schema Migrated**: Flyway migrations complete
   ```bash
   mvn flyway:info -pl northstar-persistence
   # Should show V17 migration applied
   ```

5. **Project Compiled**: All modules compile cleanly
   ```bash
   mvn clean compile
   # Should show BUILD SUCCESS
   ```

---

## Quickstart Scenarios

### Scenario 1: Single Provider Search (SearXNG - No Auth Required)

**Goal**: Verify SearXNG adapter works without API keys.

**Steps**:
1. Create DiscoverySession:
   ```java
   DiscoverySession session = discoverySessionService.createSession(SessionType.MANUAL_SEARCH);
   ```

2. Execute SearXNG search:
   ```java
   SearxngAdapter adapter = new SearxngAdapter(config, httpClient);
   Try<List<SearchResult>> result = adapter.executeSearch(
       "Bulgaria education infrastructure grants",
       20,
       session.getId()
   );
   ```

3. Verify results:
   ```java
   assertThat(result.isSuccess()).isTrue();
   assertThat(result.get()).isNotEmpty();
   assertThat(result.get()).allMatch(r -> r.getSearchEngine() == SearchEngineType.SEARXNG);
   assertThat(result.get()).allMatch(r -> r.getDomain() != null && !r.getDomain().isEmpty());
   ```

**Expected Outcome**:
- ✅ Returns 10-20 SearchResult entities
- ✅ All results have searchEngine=SEARXNG
- ✅ All results have normalized domain (lowercase, no www)
- ✅ All results have position >= 1

---

### Scenario 2: Multi-Provider Parallel Search

**Goal**: Verify all 4 providers execute concurrently and aggregate results.

**Steps**:
1. Create DiscoverySession:
   ```java
   DiscoverySession session = discoverySessionService.createSession(SessionType.NIGHTLY_DISCOVERY);
   ```

2. Execute multi-provider search:
   ```java
   Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
       "Bulgaria education grants",  // keyword query (BraveSearch, SearXNG, Serper)
       "Educational funding opportunities for schools in Bulgaria",  // AI query (Tavily)
       20,
       session.getId()
   );
   ```

3. Verify execution time:
   ```java
   long startTime = System.currentTimeMillis();
   Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(...);
   long duration = System.currentTimeMillis() - startTime;

   assertThat(duration).isLessThan(10_000); // < 10 seconds total
   ```

4. Verify results from all providers:
   ```java
   SearchExecutionResult execution = result.get();

   assertThat(execution.successfulResults()).isNotEmpty();
   assertThat(execution.statistics().braveSearchResults()).isGreaterThan(0);
   assertThat(execution.statistics().searxngResults()).isGreaterThan(0);
   assertThat(execution.statistics().serperResults()).isGreaterThan(0);
   assertThat(execution.statistics().tavilyResults()).isGreaterThan(0);
   ```

**Expected Outcome**:
- ✅ Completes in < 10 seconds (all providers parallel)
- ✅ Returns 40-80 total results (20 per provider × 4 providers, after deduplication)
- ✅ Each provider contributed results
- ✅ No duplicate domains in final result set

---

### Scenario 3: Anti-Spam Filtering

**Goal**: Verify anti-spam filter detects keyword stuffing and domain-metadata mismatch.

**Steps**:
1. Create fake spam result (keyword stuffing):
   ```java
   SearchResult spamResult = SearchResult.builder()
       .title("grants scholarships funding grants education grants financial aid grants")
       .description("grants grants grants")
       .url("https://example.com")
       .domain("example.com")
       .build();

   SpamAnalysisResult analysis = antiSpamFilter.analyzeForSpam(spamResult);
   ```

2. Verify keyword stuffing detection:
   ```java
   assertThat(analysis.isSpam()).isTrue();
   assertThat(analysis.primaryIndicator()).isEqualTo(SpamIndicator.KEYWORD_STUFFING);
   assertThat(analysis.rejectionReason()).contains("keyword stuffing");
   ```

3. Create fake spam result (domain-metadata mismatch):
   ```java
   SearchResult crossCategorySpam = SearchResult.builder()
       .title("Education Scholarships and Grants for Students")
       .description("Apply for educational funding")
       .url("https://casinowinners.com/scholarships")
       .domain("casinowinners.com")
       .build();

   SpamAnalysisResult analysis2 = antiSpamFilter.analyzeForSpam(crossCategorySpam);
   ```

4. Verify cross-category spam detection:
   ```java
   assertThat(analysis2.isSpam()).isTrue();
   assertThat(analysis2.primaryIndicator()).isEqualTo(SpamIndicator.CROSS_CATEGORY_SPAM);
   assertThat(analysis2.rejectionReason()).contains("gambling");
   ```

**Expected Outcome**:
- ✅ Keyword stuffing detected (unique word ratio < 0.5)
- ✅ Cross-category spam detected (gambling domain + education keywords)
- ✅ Both results rejected before domain deduplication

---

### Scenario 4: Domain Deduplication

**Goal**: Verify domain deduplication prevents reprocessing same organization.

**Steps**:
1. Execute first search:
   ```java
   Try<SearchExecutionResult> firstSearch = orchestrator.executeMultiProviderSearch(...);
   int firstSearchNewDomains = firstSearch.get().statistics().newDomainsDiscovered();
   ```

2. Execute second search with same query:
   ```java
   Try<SearchExecutionResult> secondSearch = orchestrator.executeMultiProviderSearch(...);
   int secondSearchNewDomains = secondSearch.get().statistics().newDomainsDiscovered();
   int secondSearchDuplicates = secondSearch.get().statistics().duplicateDomainsSkipped();
   ```

3. Verify deduplication:
   ```java
   assertThat(secondSearchNewDomains).isLessThan(firstSearchNewDomains);
   assertThat(secondSearchDuplicates).isGreaterThan(0);
   ```

4. Verify Domain entity created:
   ```java
   String testDomain = firstSearch.get().successfulResults().get(0).getDomain();
   Optional<Domain> domain = domainService.findByDomainName(testDomain);

   assertThat(domain).isPresent();
   assertThat(domain.get().getTotalOccurrences()).isEqualTo(2);  // Found in both searches
   ```

**Expected Outcome**:
- ✅ First search creates new Domain entities
- ✅ Second search marks most results as isDuplicate=true
- ✅ Domain.totalOccurrences incremented for repeated discoveries
- ✅ Domain.lastSeenAt updated to latest discovery timestamp

---

### Scenario 5: Partial Provider Failure Resilience

**Goal**: Verify system continues with successful providers when some fail.

**Steps**:
1. Configure one provider with invalid API key:
   ```properties
   search.providers.brave-search.api-key=INVALID_KEY
   ```

2. Execute multi-provider search:
   ```java
   Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(...);
   ```

3. Verify partial success:
   ```java
   assertThat(result.isSuccess()).isTrue();
   SearchExecutionResult execution = result.get();

   assertThat(execution.isPartialSuccess()).isTrue();
   assertThat(execution.successfulResults()).isNotEmpty();  // SearXNG, Serper, Tavily worked
   assertThat(execution.providerErrors()).hasSize(1);       // BraveSearch failed

   ProviderError error = execution.providerErrors().get(0);
   assertThat(error.provider()).isEqualTo(SearchEngineType.BRAVE_SEARCH);
   assertThat(error.errorType()).isEqualTo(ErrorType.AUTH_FAILURE);
   ```

4. Verify session statistics:
   ```java
   DiscoverySession session = discoverySessionService.findById(sessionId).get();

   assertThat(session.getStatus()).isEqualTo(SessionStatus.PARTIAL_SUCCESS);
   assertThat(session.getBraveSearchResults()).isEqualTo(0);  // Failed
   assertThat(session.getSearxngResults()).isGreaterThan(0);  // Worked
   assertThat(session.getSerperResults()).isGreaterThan(0);   // Worked
   assertThat(session.getTavilyResults()).isGreaterThan(0);   // Worked
   ```

**Expected Outcome**:
- ✅ Search completes successfully with 3/4 providers
- ✅ Results returned from working providers
- ✅ Error metadata captured for failed provider
- ✅ DiscoverySession status set to PARTIAL_SUCCESS

---

### Scenario 6: Rate Limiting

**Goal**: Verify rate limiter prevents exceeding API quotas.

**Steps**:
1. Configure low rate limit for testing:
   ```properties
   search.providers.serper.rate-limit.daily=2
   ```

2. Execute 3 searches in rapid succession:
   ```java
   Try<List<SearchResult>> search1 = serperAdapter.executeSearch("query1", 20, sessionId);
   Try<List<SearchResult>> search2 = serperAdapter.executeSearch("query2", 20, sessionId);
   Try<List<SearchResult>> search3 = serperAdapter.executeSearch("query3", 20, sessionId);
   ```

3. Verify rate limit enforcement:
   ```java
   assertThat(search1.isSuccess()).isTrue();
   assertThat(search2.isSuccess()).isTrue();
   assertThat(search3.isFailure()).isTrue();

   Throwable error = search3.getCause();
   assertThat(error).isInstanceOf(RateLimitException.class);
   assertThat(error.getMessage()).contains("daily quota exceeded");
   ```

4. Verify usage tracking:
   ```java
   assertThat(serperAdapter.getCurrentUsageCount()).isEqualTo(2);
   assertThat(serperAdapter.getRateLimit()).isEqualTo(2);
   ```

**Expected Outcome**:
- ✅ First 2 searches succeed
- ✅ Third search fails with RateLimitException
- ✅ Usage count tracked accurately

---

### Scenario 7: Timeout Handling

**Goal**: Verify provider timeouts don't block other providers.

**Steps**:
1. Simulate slow provider (mock or test double):
   ```java
   SearchProviderAdapter slowAdapter = new SearchProviderAdapter() {
       @Override
       public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID sessionId) {
           try {
               Thread.sleep(15_000); // 15 seconds (exceeds timeout)
               return Try.success(List.of());
           } catch (InterruptedException e) {
               return Try.failure(e);
           }
       }
       // ... other methods
   };
   ```

2. Execute multi-provider search with slow adapter:
   ```java
   long startTime = System.currentTimeMillis();
   Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(...);
   long duration = System.currentTimeMillis() - startTime;
   ```

3. Verify timeout enforcement:
   ```java
   assertThat(duration).isLessThan(11_000);  // < 11 seconds (10s timeout + margin)
   assertThat(result.isSuccess()).isTrue();  // Partial success

   SearchExecutionResult execution = result.get();
   assertThat(execution.providerErrors()).hasSize(1);

   ProviderError error = execution.providerErrors().get(0);
   assertThat(error.errorType()).isEqualTo(ErrorType.TIMEOUT);
   ```

**Expected Outcome**:
- ✅ Total search completes in < 11 seconds (enforced timeout)
- ✅ Slow provider cancelled after 10 seconds
- ✅ Other providers complete normally
- ✅ Timeout error captured in ProviderError

---

### Scenario 8: End-to-End Weekly Search Schedule Simulation

**Goal**: Simulate Monday's Bulgaria-focused search (25 categories × Bulgaria).

**Steps**:
1. Create 25 keyword queries (one per category):
   ```java
   List<String> mondayQueries = List.of(
       "Bulgaria infrastructure funding",
       "Bulgaria facility construction grants",
       "Bulgaria technology infrastructure",
       // ... 22 more categories
   );
   ```

2. Execute all queries sequentially:
   ```java
   DiscoverySession mondaySession = discoverySessionService.createSession(
       SessionType.NIGHTLY_DISCOVERY
   );

   int totalResults = 0;
   int totalNewDomains = 0;

   for (String query : mondayQueries) {
       Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
           query,  // keyword
           generateAIOptimizedQuery(query),  // AI-optimized
           20,
           mondaySession.getId()
       );

       if (result.isSuccess()) {
           totalResults += result.get().successfulResults().size();
           totalNewDomains += result.get().statistics().newDomainsDiscovered();
       }
   }
   ```

3. Verify quota consumption:
   ```java
   // 25 queries × 4 providers = 100 API calls
   assertThat(braveSearchAdapter.getCurrentUsageCount()).isLessThanOrEqualTo(50);  // Daily limit
   assertThat(serperAdapter.getCurrentUsageCount()).isLessThanOrEqualTo(60);
   assertThat(tavilyAdapter.getCurrentUsageCount()).isLessThanOrEqualTo(25);
   ```

4. Verify discovery statistics:
   ```java
   DiscoverySession completedSession = discoverySessionService.findById(mondaySession.getId()).get();

   assertThat(completedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
   assertThat(completedSession.getTotalQueriesExecuted()).isEqualTo(25);
   assertThat(completedSession.getTotalResultsFound()).isGreaterThan(100);  // ~500 results expected
   assertThat(completedSession.getNewDomainsDiscovered()).isGreaterThan(50);  // Many unique domains
   assertThat(completedSession.getSpamResultsFiltered()).isGreaterThan(200);  // 40-60% spam filtered
   ```

**Expected Outcome**:
- ✅ All 25 queries execute successfully
- ✅ 400-600 total results (20 per provider × 4 providers × 25 queries, after spam filtering)
- ✅ 40-60% spam filtering effectiveness (200-300 results filtered)
- ✅ API quotas respected (no rate limit errors)
- ✅ 100-200 new domains discovered

---

## Validation Checklist

After running all scenarios, verify:

### Functional Requirements
- ✅ All 4 search provider adapters work (Brave, SearXNG, Serper, Tavily)
- ✅ Parallel execution using Virtual Threads completes in < 10 seconds
- ✅ Anti-spam filtering detects keyword stuffing and domain-metadata mismatch
- ✅ Domain deduplication prevents reprocessing same organization
- ✅ Partial provider failures don't block successful providers
- ✅ Rate limiting prevents exceeding API quotas
- ✅ Timeouts cancel slow providers without blocking others

### Data Integrity
- ✅ SearchResult entities have all required fields populated
- ✅ Domain entities created with correct normalization (lowercase, no www)
- ✅ DiscoverySession statistics accurate (match actual results)
- ✅ isDuplicate flag set correctly on SearchResult entities
- ✅ Domain.totalOccurrences incremented on repeated discoveries

### Performance
- ✅ Multi-provider search completes in < 10 seconds (90th percentile)
- ✅ Anti-spam filtering completes in < 5ms per result
- ✅ Virtual Thread parallel execution working (no sequential execution)

### Error Handling
- ✅ Vavr Try monad used consistently (no null returns)
- ✅ Resilience4j retry working for transient failures
- ✅ Rate limit errors handled gracefully
- ✅ Authentication errors reported clearly
- ✅ Timeout errors don't cascade to other providers

---

## Success Criteria

This feature is **READY FOR PRODUCTION** when:

1. ✅ All 8 quickstart scenarios pass
2. ✅ All validation checklist items pass
3. ✅ Integration tests cover all scenarios (TestContainers)
4. ✅ Unit tests achieve >90% code coverage
5. ✅ No NEEDS CLARIFICATION markers remain in spec
6. ✅ All constitutional principles satisfied

---

**Quickstart Complete**: Ready for Phase 2 (Task Planning)
**Next**: Use `/tasks` command to generate implementation tasks
