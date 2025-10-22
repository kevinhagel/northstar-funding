# Quickstart Guide: AI-Powered Query Generation and Metadata Judging

**Feature**: 004-ai-query-generation-metadata-judging
**Date**: 2025-10-21
**Phase**: Implementation Quick Reference

---

## Overview

This quickstart guide provides concrete examples and code snippets for implementing Feature 004. Use this alongside spec.md (requirements), plan.md (architecture), and tasks.md (task breakdown).

---

## TL;DR - What This Feature Does

```
Before Feature 004:
  - 16 hardcoded queries in application.yml
  - No quality filtering
  - All search results → manual review

After Feature 004:
  - 10-20 AI-generated queries per day (diverse, targeted)
  - Metadata-based confidence scoring (0.00-1.00)
  - High-confidence (>= 0.60) → Automatic candidate creation
  - Low-confidence (< 0.60) → Skipped, saves processing
  - Domain quality tracking → Learn which domains are good
```

---

## Quick Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│ 1:00 AM - QueryGenerationScheduler                             │
│                                                                 │
│  LM Studio (Mac Studio:1234)                                  │
│      ↓                                                          │
│  "Generate 10 search queries for Bulgaria education grants"   │
│      ↓                                                          │
│  AI Response:                                                   │
│    - "Bulgaria university scholarships 2025"                   │
│    - "Eastern Europe STEM education funding"                   │
│    - "Bulgaria ministry of education grants"                   │
│    ... (10-20 queries)                                         │
│      ↓                                                          │
│  QueryGenerationService validates & stores                     │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ 2:00 AM - NightlyDiscoveryScheduler                            │
│                                                                 │
│  Load queries (AI-generated + hardcoded)                       │
│      ↓                                                          │
│  Execute across 3 engines (Searxng, Tavily, Perplexity)       │
│      ↓                                                          │
│  Deduplicate by domain (Feature 003)                           │
│      ↓                                                          │
│  200-400 unique domains                                        │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ 2:15 AM - MetadataJudgingService                               │
│                                                                 │
│  For each search result:                                       │
│    1. Extract domain (e.g., "education.gov.bg")               │
│    2. Check blacklist (skip if blacklisted)                    │
│    3. Calculate confidence score:                              │
│       - Funding keywords: 0.80 (found "grant", "scholarship")  │
│       - Domain credibility: 1.00 (.gov domain)                 │
│       - Geographic relevance: 0.90 (mentions "Bulgaria")       │
│       - Organization type: 1.00 (government)                   │
│       → Confidence: 0.925 (weighted average)                   │
│    4. Create Candidate (status: PENDING_CRAWL)                │
│    5. Update domain quality metrics                            │
│                                                                 │
│  Result:                                                        │
│    - 50-150 high-confidence candidates created                │
│    - Domain quality tiers updated                              │
└────────────────────────────────────────────────────────────────┘
```

---

## Code Examples

### 1. LM Studio API Integration

**LMStudioClient.java** (simplified):
```java
@Service
@Slf4j
public class LMStudioClient {

    private final RestClient restClient;

    public LMStudioClient(
        RestClient.Builder builder,
        @Value("${lm-studio.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "lmStudio", fallbackMethod = "generateQueriesFallback")
    @Retry(name = "lmStudio")
    public Try<List<String>> generateQueries(String prompt) {
        return Try.of(() -> {
            var request = Map.of(
                "model", "llama-3.1-8b-instruct",
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt
                )),
                "max_tokens", 500,
                "temperature", 0.7
            );

            var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(LMStudioResponse.class);

            // Parse response and extract queries (one per line)
            String content = response.choices().get(0).message().content();
            return Arrays.stream(content.split("\n"))
                .map(String::trim)
                .filter(q -> !q.isEmpty() && !q.startsWith("-"))
                .toList();
        });
    }

    private Try<List<String>> generateQueriesFallback(String prompt, Exception ex) {
        log.error("LM Studio circuit breaker triggered: {}", ex.getMessage());
        return Try.failure(new RuntimeException("LM Studio unavailable", ex));
    }

    record LMStudioResponse(
        List<Choice> choices
    ) {}

    record Choice(Message message) {}
    record Message(String content) {}
}
```

### 2. Query Generation Prompt Template

**Example Prompt**:
```
You are a funding discovery assistant. Generate 10 search queries to find funding opportunities.

Requirements:
- Geographic focus: Bulgaria, Romania, Greece, Balkans, Eastern Europe
- Category focus: Education, Infrastructure, Healthcare, STEM, Arts
- Include year 2025 when relevant
- Be specific (include organization type, program type, etc.)
- Avoid generic queries like "funding opportunities"

Format: One query per line, no numbering or bullets.

Example good queries:
- "Bulgaria university research grants 2025"
- "Romanian Ministry of Education scholarships for STEM students"
- "EU Horizon Europe funding for Balkan infrastructure projects"

Generate 10 queries now:
```

**Expected AI Response**:
```
Bulgaria education ministry grants 2025
Eastern Europe healthcare research funding opportunities
Romania STEM scholarships for university students
Balkans infrastructure development grants from EU
Greece nonprofit capacity building funding 2025
Bulgaria arts and culture grants for organizations
EU Erasmus+ education funding for Eastern Europe
Romanian government scholarships for graduate students
Bulgaria startup innovation grants 2025
Eastern Europe renewable energy research funding
```

### 3. Metadata Judging Service

**MetadataJudgingService.java** (simplified):
```java
@Service
@Slf4j
public class MetadataJudgingService {

    private final FundingKeywordsScorer fundingScorer;
    private final DomainCredibilityScorer credibilityScorer;
    private final GeographicRelevanceScorer geographicScorer;
    private final OrganizationTypeScorer orgTypeScorer;
    private final DomainRepository domainRepository;
    private final MetadataJudgmentRepository judgmentRepository;
    private final CandidateRepository candidateRepository;

    public Try<JudgingResult> judgeSearchResults(
        List<SearchResult> results,
        Long sessionId
    ) {
        return Try.of(() -> {
            var judgments = results.parallelStream()
                .map(result -> judgeOne(result, sessionId))
                .filter(Try::isSuccess)
                .map(Try::get)
                .toList();

            var highConfidence = judgments.stream()
                .filter(MetadataJudgment::isHighConfidence)
                .count();

            return new JudgingResult(
                judgments.size(),
                highConfidence,
                judgments.size() - highConfidence
            );
        });
    }

    private Try<MetadataJudgment> judgeOne(SearchResult result, Long sessionId) {
        return Try.of(() -> {
            // 1. Extract domain
            String domain = extractDomain(result.url());

            // 2. Get or create domain entity
            Domain domainEntity = domainRepository.findByDomainName(domain)
                .orElseGet(() -> createNewDomain(domain));

            // 3. Check blacklist
            if (domainEntity.getIsBlacklisted()) {
                log.debug("Skipping blacklisted domain: {}", domain);
                return null; // Will be filtered out
            }

            // 4. Calculate scores
            var fundingScore = fundingScorer.score(result.title(), result.snippet());
            var credibilityScore = credibilityScorer.score(domain);
            var geographicScore = geographicScorer.score(result.title(), result.snippet());
            var orgTypeScore = orgTypeScorer.score(result.title(), result.snippet(), domain);

            // 5. Aggregate confidence (weighted sum)
            BigDecimal confidence = calculateConfidence(
                fundingScore,
                credibilityScore,
                geographicScore,
                orgTypeScore
            );

            // 6. Create judgment record
            var judgment = MetadataJudgment.builder()
                .domainId(domainEntity.getId())
                .searchResultUrl(result.url())
                .searchResultTitle(result.title())
                .searchResultDescription(result.snippet())
                .searchEngineSource(result.source().name())
                .confidenceScore(confidence)
                .fundingKeywordsScore(fundingScore)
                .domainCredibilityScore(credibilityScore)
                .geographicRelevanceScore(geographicScore)
                .organizationTypeScore(orgTypeScore)
                .sessionId(sessionId)
                .judgingTimestamp(Instant.now())
                .build();

            judgmentRepository.save(judgment);

            // 7. Create candidate if high confidence
            if (confidence.compareTo(new BigDecimal("0.60")) >= 0) {
                createCandidate(judgment, domainEntity);
                judgment.setCandidateCreated(true);
            }

            // 8. Update domain quality metrics
            updateDomainQuality(domainEntity, confidence);

            return judgment;
        });
    }

    private BigDecimal calculateConfidence(
        BigDecimal funding,
        BigDecimal credibility,
        BigDecimal geographic,
        BigDecimal orgType
    ) {
        // Weighted average: 0.30 + 0.25 + 0.25 + 0.20 = 1.00
        BigDecimal weighted = funding.multiply(new BigDecimal("0.30"))
            .add(credibility.multiply(new BigDecimal("0.25")))
            .add(geographic.multiply(new BigDecimal("0.25")))
            .add(orgType.multiply(new BigDecimal("0.20")));

        return weighted.setScale(2, RoundingMode.HALF_UP);
    }

    private void createCandidate(MetadataJudgment judgment, Domain domain) {
        var candidate = Candidate.builder()
            .domainName(domain.getDomainName())
            .url(judgment.getSearchResultUrl())
            .status(CandidateStatus.PENDING_CRAWL)
            .confidenceScore(judgment.getConfidenceScore())
            .domainQualityTier(domain.getQualityTier().name())
            .metadataTitle(judgment.getSearchResultTitle())
            .metadataDescription(judgment.getSearchResultDescription())
            .searchEngineSource(judgment.getSearchEngineSource())
            .metadataJudgmentId(judgment.getId())
            .createdAt(Instant.now())
            .build();

        candidateRepository.save(candidate);
    }
}
```

### 4. Scoring Components

**FundingKeywordsScorer.java** (example):
```java
@Component
public class FundingKeywordsScorer {

    private static final Set<String> FUNDING_KEYWORDS = Set.of(
        "grant", "grants", "funding", "scholarship", "scholarships",
        "fellowship", "fellowships", "award", "awards", "prize",
        "stipend", "financial aid", "financial support", "bursary"
    );

    public BigDecimal score(String title, String description) {
        String combined = (title + " " + (description != null ? description : ""))
            .toLowerCase();

        // Count keywords in title (3x weight) + description (1x weight)
        int titleMatches = 0;
        int descriptionMatches = 0;

        for (String keyword : FUNDING_KEYWORDS) {
            if (title.toLowerCase().contains(keyword)) {
                titleMatches++;
            }
            if (description != null && description.toLowerCase().contains(keyword)) {
                descriptionMatches++;
            }
        }

        // Normalize to 0.00-1.00 range
        // Assume max 3 keywords in title, max 5 in description
        double score = Math.min(1.0,
            (titleMatches * 3.0 / 9.0) + (descriptionMatches * 1.0 / 5.0)
        );

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
}
```

**DomainCredibilityScorer.java** (example):
```java
@Component
public class DomainCredibilityScorer {

    public BigDecimal score(String domain) {
        domain = domain.toLowerCase();

        // High credibility
        if (domain.endsWith(".gov") || domain.endsWith(".edu")) {
            return new BigDecimal("1.00");
        }

        // Known high-quality organizations
        if (domain.contains("foundation") || domain.contains("university") ||
            domain.contains("europa.eu") || domain.contains("un.org")) {
            return new BigDecimal("0.90");
        }

        // .org domains (generally nonprofit)
        if (domain.endsWith(".org")) {
            return new BigDecimal("0.70");
        }

        // .com domains (lower trust by default)
        if (domain.endsWith(".com")) {
            // Check whitelist of known good .com domains
            if (isWhitelisted(domain)) {
                return new BigDecimal("0.60");
            }
            return new BigDecimal("0.30");
        }

        // Low credibility (blog platforms, etc.)
        if (domain.contains("blogspot") || domain.contains("wordpress") ||
            domain.contains("medium.com")) {
            return new BigDecimal("0.20");
        }

        // Default medium credibility
        return new BigDecimal("0.50");
    }

    private boolean isWhitelisted(String domain) {
        // Whitelist of known good .com domains
        return domain.equals("grants.com") ||
               domain.equals("scholarships.com") ||
               domain.equals("grantwatch.com");
    }
}
```

### 5. Domain Quality Tier Calculation

**Domain.java** (helper method):
```java
public QualityTier calculateQualityTier() {
    // Need at least 5 results to calculate tier
    if (totalResultsCount < 5) {
        return QualityTier.UNKNOWN;
    }

    double highConfidenceRatio = (double) highConfidenceCount / totalResultsCount;

    // HIGH: >70% high-confidence AND best score >= 0.70
    if (highConfidenceRatio > 0.70 &&
        bestConfidenceScore.compareTo(new BigDecimal("0.70")) >= 0) {
        return QualityTier.HIGH;
    }

    // LOW: <30% high-confidence AND best score < 0.50
    if (highConfidenceRatio < 0.30 &&
        bestConfidenceScore.compareTo(new BigDecimal("0.50")) < 0) {
        return QualityTier.LOW;
    }

    // MEDIUM: Everything else
    return QualityTier.MEDIUM;
}
```

---

## Database Query Examples

### Insert New Domain
```sql
INSERT INTO domains (
    domain_name,
    first_discovered_at,
    last_seen_at,
    best_confidence_score,
    high_confidence_count,
    low_confidence_count,
    total_results_count,
    quality_tier
) VALUES (
    'education.gov.bg',
    NOW(),
    NOW(),
    0.85,
    1,
    0,
    1,
    'UNKNOWN'
);
```

### Find High-Quality Domains
```sql
SELECT domain_name, best_confidence_score, quality_tier, total_results_count
FROM domains
WHERE quality_tier = 'HIGH'
ORDER BY best_confidence_score DESC
LIMIT 10;
```

### Blacklist Domain
```sql
UPDATE domains
SET is_blacklisted = TRUE,
    blacklist_reason = 'Spam site, no legitimate funding sources',
    blacklisted_at = NOW(),
    blacklisted_by = 'admin@northstar.com'
WHERE domain_name = 'spamsite.com';
```

### Get Judging Statistics
```sql
SELECT
    COUNT(*) as total_judgments,
    SUM(CASE WHEN confidence_score >= 0.60 THEN 1 ELSE 0 END) as high_confidence,
    SUM(CASE WHEN confidence_score < 0.60 THEN 1 ELSE 0 END) as low_confidence,
    AVG(confidence_score) as avg_confidence,
    MAX(confidence_score) as max_confidence,
    MIN(confidence_score) as min_confidence
FROM metadata_judgments
WHERE session_id = 123;
```

---

## Testing Examples

### Test Case 1: High-Confidence Result

**Input**:
```java
SearchResult result = new SearchResult(
    "https://education.gov.bg/grants/stem-2025",
    "Bulgaria Ministry of Education - STEM Scholarships 2025",
    "The Ministry of Education offers scholarships and grants for Bulgarian students pursuing STEM degrees at universities.",
    SearchEngineType.TAVILY,
    "Bulgaria STEM education grants",
    1,
    Instant.now()
);
```

**Expected Output**:
```
Confidence Score: 0.85-0.95
- Funding keywords: 0.90 (found "scholarships", "grants")
- Domain credibility: 1.00 (.gov domain)
- Geographic relevance: 1.00 (mentions "Bulgaria", "Bulgarian")
- Organization type: 1.00 (government ministry)

Candidate Created: YES (PENDING_CRAWL)
Domain Quality Tier: HIGH (after multiple high scores)
```

### Test Case 2: Low-Confidence Result

**Input**:
```java
SearchResult result = new SearchResult(
    "https://personalblog.blogspot.com/how-to-get-money",
    "How to Get Free Money Online",
    "Tips and tricks for finding funding opportunities on the internet.",
    SearchEngineType.SEARXNG,
    "funding opportunities",
    15,
    Instant.now()
);
```

**Expected Output**:
```
Confidence Score: 0.15-0.25
- Funding keywords: 0.30 (found "funding")
- Domain credibility: 0.20 (.blogspot domain)
- Geographic relevance: 0.00 (no geographic terms)
- Organization type: 0.10 (personal blog)

Candidate Created: NO
Domain Quality Tier: LOW (after multiple low scores)
```

---

## Performance Benchmarks

### Query Generation
```
Input: 20 queries requested
LM Studio Response Time: 10-25 seconds
Validation Time: <1 second
Total Time: 11-26 seconds

Queries Approved: 15-18 (75-90% approval rate)
Queries Rejected: 2-5 (too generic or duplicate)
```

### Metadata Judging
```
Input: 200 search results
Processing Time (parallel): 2-4 seconds
Avg Time per Result: 10-20ms

High-Confidence: 40-80 (20-40%)
Low-Confidence: 120-160 (60-80%)
Candidates Created: 40-80
```

### Domain Quality Update
```
Input: 80 domain updates
Processing Time: 0.5-1 second
Avg Time per Domain: 6-12ms
```

---

## Troubleshooting

### LM Studio Not Responding

**Symptom**: `Connection refused to http://192.168.1.10:1234`

**Solution**:
```bash
# Check LM Studio is running on Mac Studio
ssh macstudio "curl -s http://localhost:1234/v1/models"

# Start LM Studio if not running
# Open LM Studio app on Mac Studio
# Load model: llama-3.1-8b-instruct
# Start local server on port 1234
```

### All Queries Rejected as Duplicates

**Symptom**: `QueryGenerationService rejected all 20 queries`

**Solution**:
- Lower similarity threshold in configuration
- Clear old queries from database: `DELETE FROM search_queries WHERE generation_date < NOW() - INTERVAL '14 days'`
- Use more diverse prompt templates

### Low Precision (<70%)

**Symptom**: Manual review shows many false positives

**Solution**:
1. Collect 50 false positive examples
2. Analyze common patterns
3. Adjust scoring weights:
   - Increase domain_credibility weight if many spam domains
   - Increase geographic_relevance if many non-Eastern Europe results
   - Add negative keywords (exclude "loan", "casino", etc.)

---

## Next Steps After Implementation

1. **Monitor Metrics**:
   - Track precision (manual review pass rate)
   - Track query diversity (uniqueness from previous week)
   - Track domain quality tier distribution

2. **Tune Scoring Weights**:
   - Collect feedback from manual reviews
   - Adjust weights based on precision/recall
   - Document final configuration

3. **Expand Query Templates**:
   - Add more geographic focuses
   - Add more category focuses
   - Test different prompt engineering approaches

4. **Prepare for Feature 005**:
   - Candidates with PENDING_CRAWL status ready for deep crawling
   - Domain quality tiers can prioritize which to crawl first
   - robots.txt and sitemap.xml processing

---

**Quickstart Status**: ✅ COMPLETE - Ready for implementation reference
