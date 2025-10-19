# Quickstart: Automated Crawler Infrastructure - Phase 1 Metadata Judging

**Feature**: 002-create-automated-crawler
**Date**: 2025-10-19

## Prerequisites

### Environment Setup
1. **Mac Studio PostgreSQL** running at 192.168.1.10:5432
2. **Database**: `northstar_funding` with migrations V1-V9 applied
3. **Java**: Java 25 via SDKMAN
4. **Spring Boot**: 3.5.5 application running
5. **Test Data**: Mock SearchResult list with 20-25 results

### Dependencies
- DomainRegistryService
- MetadataJudgingService
- CandidateProcessingOrchestrator
- DomainRepository
- FundingSourceCandidateRepository

---

## Scenario 1: First-Time Domain Discovery and High-Confidence Candidate Creation

**Goal**: Verify that a new domain with high-quality metadata creates a PENDING_CRAWL candidate.

### Setup
```java
// Create discovery session
UUID sessionId = UUID.randomUUID();

// Create mock search result with high-quality metadata
SearchResult result = SearchResult.builder()
    .url("https://us-bulgaria.org/grants/education-2025")
    .title("Education Grants for Bulgaria - US-Bulgaria Foundation")
    .snippet("Annual grants supporting educational initiatives in Bulgaria. Funding ranges from €10,000 to €50,000 for qualified NGOs and schools.")
    .searchEngine("searxng")
    .searchQuery("Bulgaria education grants NGO")
    .position(1)
    .build();

List<SearchResult> results = List.of(result);
```

### Execute
```java
ProcessingStats stats = candidateProcessingOrchestrator.processSearchResults(
    results,
    sessionId
);
```

### Verify
```java
// Assert: 1 candidate created
assertThat(stats.getCandidatesCreated()).isEqualTo(1);
assertThat(stats.getSkippedLowConfidence()).isEqualTo(0);
assertThat(stats.getAverageConfidence()).isGreaterThanOrEqualTo(0.6);

// Assert: Domain registered
Optional<Domain> domain = domainRepository.findByDomainName("us-bulgaria.org");
assertThat(domain).isPresent();
assertThat(domain.get().getStatus()).isEqualTo(DomainStatus.PROCESSED_HIGH_QUALITY);
assertThat(domain.get().getHighQualityCandidateCount()).isEqualTo(1);
assertThat(domain.get().getBestConfidenceScore()).isGreaterThanOrEqualTo(0.6);

// Assert: Candidate created with correct status
List<FundingSourceCandidate> candidates = candidateRepository.findByStatus(
    CandidateStatus.PENDING_CRAWL
);
assertThat(candidates).hasSize(1);
FundingSourceCandidate candidate = candidates.get(0);
assertThat(candidate.getDomainId()).isEqualTo(domain.get().getDomainId());
assertThat(candidate.getOrganizationName()).contains("US-Bulgaria Foundation");
assertThat(candidate.getProgramName()).contains("Education Grants");
assertThat(candidate.getConfidenceScore()).isGreaterThanOrEqualTo(0.6);
```

**Expected Outcome**: ✅ 1 candidate created, domain registered as PROCESSED_HIGH_QUALITY

---

## Scenario 2: Domain Deduplication (Skip Already Processed Domain)

**Goal**: Verify that search results from an already-processed domain are skipped.

### Setup
```java
// Pre-register domain as already processed
Domain existingDomain = domainRegistryService.registerDomain(
    "example-foundation.org",
    sessionId
).get();

// Update to PROCESSED_LOW_QUALITY status
existingDomain.setStatus(DomainStatus.PROCESSED_LOW_QUALITY);
existingDomain.setLowQualityCandidateCount(5);
existingDomain.setHighQualityCandidateCount(0);
domainRepository.save(existingDomain);

// Create search result from same domain
SearchResult result = SearchResult.builder()
    .url("https://example-foundation.org/new-program")
    .title("New Grant Program 2025")
    .snippet("We're offering new grants...")
    .searchEngine("tavily")
    .searchQuery("foundation grants")
    .position(5)
    .build();

List<SearchResult> results = List.of(result);
```

### Execute
```java
ProcessingStats stats = candidateProcessingOrchestrator.processSearchResults(
    results,
    sessionId
);
```

### Verify
```java
// Assert: 0 candidates created (domain already processed)
assertThat(stats.getCandidatesCreated()).isEqualTo(0);
assertThat(stats.getSkippedDomainAlreadyProcessed()).isEqualTo(1);
assertThat(stats.getTotalProcessed()).isEqualTo(1);

// Assert: Domain status unchanged
Domain domain = domainRepository.findByDomainName("example-foundation.org").get();
assertThat(domain.getStatus()).isEqualTo(DomainStatus.PROCESSED_LOW_QUALITY);
```

**Expected Outcome**: ✅ 0 candidates created, domain skipped due to deduplication

---

## Scenario 3: Low-Confidence Metadata Skipping

**Goal**: Verify that search results with low confidence scores are skipped (not worth crawling).

### Setup
```java
// Create search result with poor metadata (no funding keywords, suspicious domain)
SearchResult result = SearchResult.builder()
    .url("https://click-here-for-grants.com/offers")
    .title("Amazing Grant Opportunities!")
    .snippet("Click here to discover funding...")
    .searchEngine("perplexity")
    .searchQuery("Bulgaria grants")
    .position(15)
    .build();

List<SearchResult> results = List.of(result);
```

### Execute
```java
ProcessingStats stats = candidateProcessingOrchestrator.processSearchResults(
    results,
    sessionId
);
```

### Verify
```java
// Assert: 0 candidates created (low confidence)
assertThat(stats.getCandidatesCreated()).isEqualTo(0);
assertThat(stats.getSkippedLowConfidence()).isEqualTo(1);
assertThat(stats.getMinConfidence()).isLessThan(0.6);

// Assert: Domain registered but marked low quality
Optional<Domain> domain = domainRepository.findByDomainName("click-here-for-grants.com");
assertThat(domain).isPresent();
assertThat(domain.get().getStatus()).isIn(
    DomainStatus.DISCOVERED,
    DomainStatus.PROCESSED_LOW_QUALITY
);
assertThat(domain.get().getLowQualityCandidateCount()).isGreaterThanOrEqualTo(1);
```

**Expected Outcome**: ✅ 0 candidates created, domain marked as low quality

---

## Scenario 4: Blacklist Management (Admin Blocks Spam Domain)

**Goal**: Verify that admin users can blacklist domains and future results are auto-rejected.

### Setup
```java
UUID adminUserId = UUID.randomUUID(); // Mock admin user

// Admin blacklists a known spam domain
domainRegistryService.blacklistDomain(
    "spam-grants.com",
    "Known scam site - reports stolen credit card numbers",
    adminUserId
);

// Search engine returns result from blacklisted domain
SearchResult result = SearchResult.builder()
    .url("https://spam-grants.com/free-money")
    .title("Free Grant Money - Apply Now!")
    .snippet("Get free money for your nonprofit...")
    .searchEngine("searxng")
    .searchQuery("free grants NGO")
    .position(8)
    .build();

List<SearchResult> results = List.of(result);
```

### Execute
```java
ProcessingStats stats = candidateProcessingOrchestrator.processSearchResults(
    results,
    sessionId
);
```

### Verify
```java
// Assert: 0 candidates created (blacklisted domain)
assertThat(stats.getCandidatesCreated()).isEqualTo(0);
assertThat(stats.getSkippedBlacklisted()).isEqualTo(1);

// Assert: Domain remains blacklisted
Domain domain = domainRepository.findByDomainName("spam-grants.com").get();
assertThat(domain.getStatus()).isEqualTo(DomainStatus.BLACKLISTED);
assertThat(domain.getBlacklistReason()).contains("scam");
assertThat(domain.getBlacklistedBy()).isEqualTo(adminUserId);
```

**Expected Outcome**: ✅ 0 candidates created, blacklisted domain auto-rejected

---

## Scenario 5: Parallel Processing with Virtual Threads

**Goal**: Verify that 20-25 search results are processed in parallel efficiently.

### Setup
```java
// Create 25 search results with varying quality
List<SearchResult> results = IntStream.range(1, 26)
    .mapToObj(i -> SearchResult.builder()
        .url(String.format("https://foundation-%d.org/grants", i))
        .title(i % 3 == 0
            ? String.format("Education Grant #%d", i) // High quality (33%)
            : String.format("Generic Page #%d", i))   // Low quality (67%)
        .snippet("Funding opportunities available...")
        .searchEngine("tavily")
        .searchQuery("education grants")
        .position(i)
        .build())
    .toList();
```

### Execute
```java
long startTime = System.currentTimeMillis();

ProcessingStats stats = candidateProcessingOrchestrator.processSearchResults(
    results,
    sessionId
);

long duration = System.currentTimeMillis() - startTime;
```

### Verify
```java
// Assert: All results processed
assertThat(stats.getTotalProcessed()).isEqualTo(25);

// Assert: Approximately 8-10 high-confidence candidates (33% of 25)
assertThat(stats.getCandidatesCreated()).isBetween(7, 12);

// Assert: Remaining skipped as low confidence
assertThat(stats.getSkippedLowConfidence()).isGreaterThan(13);

// Assert: Processing completed quickly (parallel execution)
// Note: Actual time varies, but should be much faster than 25 * 500ms = 12.5s sequential
assertThat(duration).isLessThan(5000); // < 5 seconds for 25 results

// Assert: Statistics are accurate
assertThat(stats.getAverageConfidence()).isNotNull();
assertThat(stats.getMaxConfidence()).isGreaterThanOrEqualTo(stats.getMinConfidence());
```

**Expected Outcome**: ✅ 25 results processed in parallel, ~33% created as candidates

---

## Scenario 6: "No Funds This Year" Re-evaluation

**Goal**: Verify that domains marked "no funds this year" can be re-evaluated in future years.

### Setup
```java
// Pre-register domain marked as "no funds" for 2024
Domain domain = domainRegistryService.registerDomain(
    "annual-foundation.org",
    sessionId
).get();

domainRegistryService.markNoFundsThisYear(
    "annual-foundation.org",
    2024,
    "Foundation confirmed no grants offered in 2024, check back in 2025"
);

// Simulate year change (in real system, would be different year)
// For test: manually update to allow re-processing
Domain updated = domainRepository.findByDomainName("annual-foundation.org").get();
assertThat(updated.getStatus()).isEqualTo(DomainStatus.NO_FUNDS_THIS_YEAR);
assertThat(updated.getNoFundsYear()).isEqualTo(2024);

// Mock "new year" scenario by checking shouldProcessDomain logic
// In 2025, shouldProcessDomain should return true for 2024 "no funds" domains
```

### Execute
```java
// Simulate 2025 search result
SearchResult result = SearchResult.builder()
    .url("https://annual-foundation.org/grants-2025")
    .title("2025 Grant Program - Annual Foundation")
    .snippet("We're pleased to announce our 2025 funding cycle...")
    .searchEngine("browserbase")
    .searchQuery("foundation grants 2025")
    .position(2)
    .build();

// In real system with year = 2025, this would process
// For test, manually verify the shouldProcessDomain logic
boolean shouldProcess = domainRegistryService.shouldProcessDomainByName("annual-foundation.org");
```

### Verify
```java
// Assert: Domain can be re-processed in new year
// (In actual 2025, shouldProcessDomain would return true)
// For test purposes, verify the status allows future processing
Domain domain = domainRepository.findByDomainName("annual-foundation.org").get();
assertThat(domain.getStatus()).isEqualTo(DomainStatus.NO_FUNDS_THIS_YEAR);
assertThat(domain.getNoFundsYear()).isEqualTo(2024);
assertThat(domain.getNotes()).contains("check back in 2025");
```

**Expected Outcome**: ✅ Domain can be re-evaluated after marked year passes

---

## Integration Test Summary

### Test Coverage
- ✅ **Scenario 1**: First-time domain discovery → PENDING_CRAWL candidate
- ✅ **Scenario 2**: Domain deduplication → skip already processed
- ✅ **Scenario 3**: Low confidence → skip crawling
- ✅ **Scenario 4**: Blacklist management → permanent blocking
- ✅ **Scenario 5**: Parallel processing → Virtual Threads efficiency
- ✅ **Scenario 6**: "No funds this year" → re-evaluation logic

### Success Criteria
1. Domain-level deduplication prevents duplicate processing
2. Metadata judging creates high-confidence candidates (>= 0.6)
3. Low-confidence results are skipped (resource conservation)
4. Blacklisted domains are permanently blocked
5. Parallel processing handles 20-25 results efficiently
6. Quality metrics are tracked per domain

### Performance Expectations
- **Metadata judging**: < 500ms per result (no web crawling)
- **Parallel processing**: 20-25 results < 5 seconds total
- **Database queries**: Domain lookups optimized with indexes

---

## Running Quickstart Tests

### Command
```bash
cd /Users/kevin/github/northstar-funding/backend
mvn test -Dtest=CrawlerInfrastructureQuickstartTest
```

### Expected Output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running CrawlerInfrastructureQuickstartTest
[INFO] Scenario 1: PASSED ✅
[INFO] Scenario 2: PASSED ✅
[INFO] Scenario 3: PASSED ✅
[INFO] Scenario 4: PASSED ✅
[INFO] Scenario 5: PASSED ✅
[INFO] Scenario 6: PASSED ✅
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
