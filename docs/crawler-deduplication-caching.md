# Crawler Deduplication & Caching Strategy
## Domain-Based Deduplication with Redis/Valkey

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Design Phase

---

## Core Principle: Domain-Level Deduplication

**Key Insight**: Organizations have a single domain, but many URLs

```
Example: America for Bulgaria Foundation

Domain: us-bulgaria.org

URLs discovered:
- https://us-bulgaria.org/grants
- https://us-bulgaria.org/education-program
- https://us-bulgaria.org/about
- https://us-bulgaria.org/contact

Action: Process ONCE per domain, not once per URL
```

**Why Domain-Level?**
1. Organizations are identified by domain
2. Multiple URLs from same domain = same organization
3. Homepage is entry point for humans to gather org info
4. Prevents duplicate processing of same funder

---

## Search Engine Integration

### Available Search Engines

**1. Browserbase** (API Key available)
- Type: AI-generated keyword searches
- Use Case: Targeted keyword queries
- API: Yes

**2. Searxng** (Installed on Mac Studio 192.168.1.10)
- Type: AI-generated keyword searches
- Use Case: Privacy-focused meta-search
- Local deployment

**3. Tavily** (Account available)
- Type: AI-generated prompts
- Use Case: AI-powered search for specific information
- API: Yes

**4. Perplexity** (Account available)
- Type: AI-generated prompts
- Use Case: Research-oriented AI search
- API: Yes

### Search Strategy Categorization

**Example - Today's Search**:
```
Target: Eastern Europe and EU funding candidates

Browserbase Queries (AI-generated keywords):
- "education grants Bulgaria"
- "EU Cohesion Fund Eastern Europe"
- "Romania school infrastructure funding"

Searxng Queries (AI-generated keywords):
- "scholarships Bulgaria 2025"
- "Balkans education funding programs"
- "Polish NGO grants"

Tavily Prompts (AI-generated):
- "Find foundations providing education grants in Eastern Europe"
- "What EU programs fund infrastructure in Bulgaria and Romania?"

Perplexity Prompts (AI-generated):
- "List organizations offering grants for schools in the Balkans"
- "Find US foundations funding education in Eastern Europe"
```

**Weekly Rotation** (from architecture-crawler-hybrid.md):
- Monday: EU Programs
- Tuesday: US Foundations (Eastern Europe focus)
- Wednesday: Corporate Foundations
- Thursday: Multilateral Organizations
- Friday: Bulgarian National Programs
- Saturday: Regional/Local
- Sunday: Aggregators & Exploratory

---

## Deduplication Flow

### Step 1: Search Engines Return URLs

```
Browserbase finds:
- https://us-bulgaria.org/grants
- https://erasmus-plus.ec.europa.eu/programme-guide

Searxng finds:
- https://us-bulgaria.org/about
- https://ec.europa.eu/regional_policy/funding

Tavily finds:
- https://us-bulgaria.org/education-program

Perplexity finds:
- https://erasmus-plus.ec.europa.eu/opportunities
```

### Step 2: Extract Domains

```
URLs → Domains:

us-bulgaria.org (from 3 different URLs)
erasmus-plus.ec.europa.eu (from 2 different URLs)
ec.europa.eu (from 1 URL)
```

### Step 3: Check Redis Cache (Deduplicate)

```
FOR EACH domain:
  1. Check Redis: Has this domain been processed?

  IF domain is in cache:
    - Status: "processing" → Skip (another worker is processing it)
    - Status: "processed_today" → Skip (already processed today)
    - Status: "processed_this_week" → Skip (weekly slot not yet)
    - Status: "blacklisted" → Skip (marked as not worth it)

  ELSE:
    - Mark as "processing" in Redis
    - Add to crawl queue
```

### Step 4: Crawl Domain Homepage

```
For domain: us-bulgaria.org

1. Fetch homepage: https://us-bulgaria.org
2. Extract organization info:
   - Name: "America for Bulgaria Foundation"
   - Mission statement
   - Contact info (emails, phones, addresses)
   - "About Us" URL
   - "Contact Us" URL
   - Programs (if listed on homepage)

3. Look for program links:
   - "Education" → https://us-bulgaria.org/education-program
   - "Grants" → https://us-bulgaria.org/grants

   Note: May be in dynamic menus (JavaScript) → Hard to scrape
   → This is where HUMAN comes in (next step)

4. Create FundingSourceCandidate:
   - organizationName: "America for Bulgaria Foundation"
   - sourceUrl: "https://us-bulgaria.org"
   - extractedData: { homepage content, links found }
   - status: PENDING_REVIEW

5. Mark domain as "processed_today" in Redis
```

---

## Redis/Valkey Caching Schema

### Cache Keys

**Pattern**: `crawler:domain:{domain}:{date_or_week}`

**Examples**:
```
crawler:domain:us-bulgaria.org:2025-10-18        # Processed today
crawler:domain:us-bulgaria.org:2025-W42          # Processed this week (week 42)
crawler:domain:us-bulgaria.org:blacklisted       # Blacklisted
crawler:domain:us-bulgaria.org:status            # Current processing status
```

### Cache Values (JSON)

**Processing Status**:
```json
{
  "domain": "us-bulgaria.org",
  "status": "processing",
  "started_at": "2025-10-18T02:30:00Z",
  "worker_id": "crawler-worker-1",
  "discovery_session_id": "uuid"
}
```

**Processed Today**:
```json
{
  "domain": "us-bulgaria.org",
  "status": "processed_today",
  "processed_at": "2025-10-18T02:45:00Z",
  "candidate_id": "uuid",
  "discovery_session_id": "uuid"
}
```

**Processed This Week** (for weekly rotation):
```json
{
  "domain": "us-bulgaria.org",
  "status": "processed_this_week",
  "processed_at": "2025-10-18T02:45:00Z",
  "week": "2025-W42",
  "candidate_id": "uuid",
  "next_process_after": "2025-10-25T00:00:00Z"
}
```

**Blacklisted**:
```json
{
  "domain": "scam-grants.com",
  "status": "blacklisted",
  "reason": "Known scam site",
  "blacklisted_by": "kevin@example.com",
  "blacklisted_at": "2025-10-15T10:30:00Z"
}
```

**No Funds This Year** (temporary status):
```json
{
  "domain": "local-foundation.bg",
  "status": "no_funds_2025",
  "reason": "Fulfilled provider goals for 2025, no funds available",
  "revisit_after": "2026-01-01T00:00:00Z",
  "noted_by": "huw@example.com",
  "noted_at": "2025-10-10T14:00:00Z"
}
```

### Cache TTL (Time-To-Live)

**Daily Processing**:
```
Key: crawler:domain:us-bulgaria.org:2025-10-18
TTL: 24 hours (expires at midnight)
```

**Weekly Processing**:
```
Key: crawler:domain:us-bulgaria.org:2025-W42
TTL: 7 days
```

**Blacklist**:
```
Key: crawler:domain:scam-grants.com:blacklisted
TTL: No expiration (manual removal only)
```

**No Funds This Year**:
```
Key: crawler:domain:local-foundation.bg:no_funds_2025
TTL: Until "revisit_after" date
```

---

## Domain Processing States

### State Machine

```
[DISCOVERED]
    │
    ├─ Check cache
    │
    ├─ IF "processing" → SKIP (wait for other worker to finish)
    │
    ├─ IF "processed_today" → SKIP (already done)
    │
    ├─ IF "processed_this_week" → SKIP (wait for next week)
    │
    ├─ IF "blacklisted" → SKIP (never process)
    │
    ├─ IF "no_funds_2025" → SKIP (wait until revisit_after date)
    │
    └─ IF NOT IN CACHE → PROCESS
           │
           ├─ Mark as "processing"
           │
           ├─ Crawl homepage
           │
           ├─ Extract organization info
           │
           ├─ Create FundingSourceCandidate
           │
           ├─ Mark as "processed_today" or "processed_this_week"
           │
           └─ [PENDING_REVIEW]
```

---

## Deduplication Implementation

### Service Layer

```java
@Service
public class DomainDeduplicationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Check if domain should be processed
     */
    public boolean shouldProcess(String domain) {
        // Check daily cache
        String dailyKey = buildDailyKey(domain);
        if (redisTemplate.hasKey(dailyKey)) {
            return false; // Already processed today
        }

        // Check weekly cache (for weekly rotation)
        String weeklyKey = buildWeeklyKey(domain);
        if (redisTemplate.hasKey(weeklyKey)) {
            return false; // Already processed this week
        }

        // Check blacklist
        String blacklistKey = buildBlacklistKey(domain);
        if (redisTemplate.hasKey(blacklistKey)) {
            return false; // Blacklisted
        }

        // Check "no funds this year"
        String noFundsKey = buildNoFundsKey(domain);
        if (redisTemplate.hasKey(noFundsKey)) {
            DomainStatus status = getStatus(noFundsKey);
            LocalDateTime revisitAfter = status.getRevisitAfter();
            if (LocalDateTime.now().isBefore(revisitAfter)) {
                return false; // Too early to revisit
            }
        }

        // Check if currently being processed
        String statusKey = buildStatusKey(domain);
        if (redisTemplate.hasKey(statusKey)) {
            DomainStatus status = getStatus(statusKey);
            if ("processing".equals(status.getStatus())) {
                return false; // Another worker is processing
            }
        }

        return true; // OK to process
    }

    /**
     * Mark domain as being processed
     */
    public void markProcessing(String domain, UUID discoverySessionId, String workerId) {
        String key = buildStatusKey(domain);
        DomainStatus status = DomainStatus.builder()
            .domain(domain)
            .status("processing")
            .startedAt(LocalDateTime.now())
            .workerId(workerId)
            .discoverySessionId(discoverySessionId)
            .build();

        redisTemplate.opsForValue().set(
            key,
            toJson(status),
            Duration.ofHours(1) // Expire after 1 hour (if worker crashes)
        );
    }

    /**
     * Mark domain as processed (daily)
     */
    public void markProcessedToday(String domain, UUID candidateId, UUID discoverySessionId) {
        String key = buildDailyKey(domain);
        DomainStatus status = DomainStatus.builder()
            .domain(domain)
            .status("processed_today")
            .processedAt(LocalDateTime.now())
            .candidateId(candidateId)
            .discoverySessionId(discoverySessionId)
            .build();

        redisTemplate.opsForValue().set(
            key,
            toJson(status),
            Duration.ofDays(1) // Expire at end of day
        );

        // Remove "processing" status
        redisTemplate.delete(buildStatusKey(domain));
    }

    /**
     * Mark domain as processed (weekly)
     */
    public void markProcessedThisWeek(String domain, UUID candidateId, UUID discoverySessionId) {
        String key = buildWeeklyKey(domain);
        DomainStatus status = DomainStatus.builder()
            .domain(domain)
            .status("processed_this_week")
            .processedAt(LocalDateTime.now())
            .week(getCurrentWeek())
            .candidateId(candidateId)
            .discoverySessionId(discoverySessionId)
            .nextProcessAfter(LocalDateTime.now().plusWeeks(1))
            .build();

        redisTemplate.opsForValue().set(
            key,
            toJson(status),
            Duration.ofDays(7)
        );

        // Remove "processing" status
        redisTemplate.delete(buildStatusKey(domain));
    }

    /**
     * Blacklist a domain
     */
    public void blacklist(String domain, String reason, String blacklistedBy) {
        String key = buildBlacklistKey(domain);
        DomainStatus status = DomainStatus.builder()
            .domain(domain)
            .status("blacklisted")
            .reason(reason)
            .blacklistedBy(blacklistedBy)
            .blacklistedAt(LocalDateTime.now())
            .build();

        redisTemplate.opsForValue().set(key, toJson(status));
        // No TTL - persists until manually removed
    }

    /**
     * Mark domain as "no funds this year"
     */
    public void markNoFundsThisYear(String domain, String reason, LocalDateTime revisitAfter, String notedBy) {
        String key = buildNoFundsKey(domain);
        DomainStatus status = DomainStatus.builder()
            .domain(domain)
            .status("no_funds_2025")
            .reason(reason)
            .revisitAfter(revisitAfter)
            .notedBy(notedBy)
            .notedAt(LocalDateTime.now())
            .build();

        long ttlSeconds = Duration.between(LocalDateTime.now(), revisitAfter).getSeconds();
        redisTemplate.opsForValue().set(
            key,
            toJson(status),
            Duration.ofSeconds(ttlSeconds)
        );
    }

    // Key builders
    private String buildDailyKey(String domain) {
        String date = LocalDate.now().toString(); // 2025-10-18
        return String.format("crawler:domain:%s:%s", domain, date);
    }

    private String buildWeeklyKey(String domain) {
        String week = getCurrentWeek(); // 2025-W42
        return String.format("crawler:domain:%s:%s", domain, week);
    }

    private String buildStatusKey(String domain) {
        return String.format("crawler:domain:%s:status", domain);
    }

    private String buildBlacklistKey(String domain) {
        return String.format("crawler:domain:%s:blacklisted", domain);
    }

    private String buildNoFundsKey(String domain) {
        return String.format("crawler:domain:%s:no_funds_2025", domain);
    }

    private String getCurrentWeek() {
        // ISO week: 2025-W42
        return LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-'W'ww"));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private DomainStatus getStatus(String key) {
        String json = redisTemplate.opsForValue().get(key);
        try {
            return objectMapper.readValue(json, DomainStatus.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Domain Extraction Utility

```java
@Component
public class DomainExtractor {

    /**
     * Extract domain from URL
     *
     * Examples:
     * - https://us-bulgaria.org/grants → us-bulgaria.org
     * - https://erasmus-plus.ec.europa.eu/opportunities → erasmus-plus.ec.europa.eu
     * - https://www.gatesfoundation.org/about → gatesfoundation.org (strip www)
     */
    public String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            // Strip "www." prefix
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }
}
```

---

## Search Engine Workflow with Deduplication

### Complete Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  AI QUERY GENERATION                             │
│  LLM generates queries for today's target:                       │
│  "Eastern Europe and EU funding candidates"                      │
│                                                                  │
│  Output:                                                         │
│  - Keywords: ["education grants Bulgaria", "EU Cohesion Fund"]  │
│  - Prompts: ["Find foundations funding education in Bulgaria"]  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              PARALLEL SEARCH EXECUTION                           │
│                                                                  │
│  Browserbase:                                                    │
│  - "education grants Bulgaria"                                   │
│  → Returns: us-bulgaria.org/grants, erasmus-plus.ec.europa.eu   │
│                                                                  │
│  Searxng:                                                        │
│  - "EU Cohesion Fund Eastern Europe"                             │
│  → Returns: us-bulgaria.org/about, ec.europa.eu/regional_policy │
│                                                                  │
│  Tavily:                                                         │
│  - "Find foundations funding education in Bulgaria"              │
│  → Returns: us-bulgaria.org/education-program, ...              │
│                                                                  │
│  Perplexity:                                                     │
│  - "List organizations offering grants for schools in Balkans"   │
│  → Returns: erasmus-plus.ec.europa.eu/opportunities, ...        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              AGGREGATE & EXTRACT DOMAINS                         │
│                                                                  │
│  All URLs found:                                                 │
│  - https://us-bulgaria.org/grants                                │
│  - https://erasmus-plus.ec.europa.eu                             │
│  - https://us-bulgaria.org/about                                 │
│  - https://ec.europa.eu/regional_policy                          │
│  - https://us-bulgaria.org/education-program                     │
│  - https://erasmus-plus.ec.europa.eu/opportunities               │
│                                                                  │
│  Extract domains:                                                │
│  - us-bulgaria.org (3 URLs)                                      │
│  - erasmus-plus.ec.europa.eu (2 URLs)                            │
│  - ec.europa.eu (1 URL)                                          │
│                                                                  │
│  Deduplicated domains: 3 unique domains to process               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              CHECK REDIS CACHE (Deduplication)                   │
│                                                                  │
│  FOR domain: us-bulgaria.org                                     │
│    Check: crawler:domain:us-bulgaria.org:2025-10-18              │
│    Result: NOT FOUND → OK to process                             │
│                                                                  │
│  FOR domain: erasmus-plus.ec.europa.eu                           │
│    Check: crawler:domain:erasmus-plus.ec.europa.eu:2025-10-18    │
│    Result: FOUND (processed earlier today) → SKIP                │
│                                                                  │
│  FOR domain: ec.europa.eu                                        │
│    Check: crawler:domain:ec.europa.eu:blacklisted                │
│    Result: BLACKLISTED ("Too complex, low value") → SKIP         │
│                                                                  │
│  Domains to process: 1 (us-bulgaria.org only)                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              MARK AS PROCESSING                                  │
│                                                                  │
│  Redis SET:                                                      │
│  Key: crawler:domain:us-bulgaria.org:status                      │
│  Value: {                                                        │
│    "status": "processing",                                       │
│    "started_at": "2025-10-18T02:30:00Z",                         │
│    "worker_id": "crawler-worker-1",                              │
│    "discovery_session_id": "uuid"                                │
│  }                                                               │
│  TTL: 1 hour                                                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              CRAWL DOMAIN HOMEPAGE                               │
│                                                                  │
│  URL: https://us-bulgaria.org                                    │
│                                                                  │
│  1. Check robots.txt                                             │
│     → Allowed to crawl                                           │
│                                                                  │
│  2. Fetch homepage                                               │
│     → Extract organization name                                  │
│     → Extract mission statement                                  │
│     → Find "About Us" link                                       │
│     → Find "Contact Us" link                                     │
│     → Find program links (if visible)                            │
│                                                                  │
│  3. Store raw HTML for human review                              │
│                                                                  │
│  Note: Dynamic menus, JavaScript-rendered content → SKIP         │
│  → Humans will navigate these manually                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              CREATE CANDIDATE                                    │
│                                                                  │
│  FundingSourceCandidate:                                         │
│  - candidateId: uuid                                             │
│  - status: PENDING_REVIEW                                        │
│  - confidenceScore: 0.85                                         │
│  - discoverySessionId: uuid                                      │
│  - discoveredAt: 2025-10-18T02:45:00Z                            │
│  - discoveredBy: "SYSTEM"                                        │
│  - discoveryMethod: "CRAWLER"                                    │
│  - sourceUrl: "https://us-bulgaria.org"                          │
│  - organizationName: "America for Bulgaria Foundation"           │
│  - extractedData: { homepage HTML, links found }                 │
│                                                                  │
│  Save to PostgreSQL                                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              MARK AS PROCESSED                                   │
│                                                                  │
│  Redis SET:                                                      │
│  Key: crawler:domain:us-bulgaria.org:2025-10-18                  │
│  Value: {                                                        │
│    "status": "processed_today",                                  │
│    "processed_at": "2025-10-18T02:45:00Z",                       │
│    "candidate_id": "uuid",                                       │
│    "discovery_session_id": "uuid"                                │
│  }                                                               │
│  TTL: 24 hours (until end of day)                                │
│                                                                  │
│  Redis DELETE:                                                   │
│  Key: crawler:domain:us-bulgaria.org:status                      │
│  (Remove "processing" status)                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Human Review: Organization Homepage

### What Humans Find on Homepage

**Domain Homepage**: https://us-bulgaria.org

**Organization Information** (crawler finds):
- ✓ Organization name: "America for Bulgaria Foundation"
- ✓ Mission statement
- ✓ "About Us" link
- ✓ "Contact Us" link

**Contact Information** (humans find):
- Emails: info@us-bulgaria.org, specific program officer emails
- Phone numbers: Main office, program officers
- Addresses: Physical office locations
- Contact pages: URLs for "Contact Us" forms

**Program Information** (humans navigate):
- **Static links** (crawler can find):
  - "Education" → https://us-bulgaria.org/education-program
  - "Grants" → https://us-bulgaria.org/grants

- **Dynamic menus** (crawler misses, humans find):
  - Hover over "Programs" → Dropdown menu appears (JavaScript)
  - Click "Education" → Submenu with specific grant types
  - Navigate to individual grant pages

**Why Humans Are Needed**:
- Dynamic JavaScript menus (not visible in raw HTML)
- Interactive elements (dropdowns, tabs)
- PDFs with program information
- Complex navigation structures
- Contact forms (instead of email addresses)
- LinkedIn/social media links for staff

---

## Domain-Level Metadata & Status

### Domain Blacklisting

**Use Case**: Mark domains as not worth processing

**Examples**:
```
Domain: scam-grants.com
Reason: "Known scam site, requests upfront fees"
Action: BLACKLIST

Domain: too-complex-portal.gov
Reason: "Requires government account, not accessible to NGOs"
Action: BLACKLIST

Domain: outdated-foundation.org
Reason: "Foundation dissolved in 2020, website still up"
Action: BLACKLIST
```

**Implementation**:
```java
// Human marks candidate as blacklist
@PostMapping("/api/candidates/{id}/blacklist")
public void blacklistDomain(@PathVariable UUID id, @RequestBody BlacklistRequest request) {
    FundingSourceCandidate candidate = candidateRepo.findById(id);
    String domain = domainExtractor.extractDomain(candidate.getSourceUrl());

    // Mark domain in Redis
    deduplicationService.blacklist(
        domain,
        request.getReason(),
        currentUser.getEmail()
    );

    // Mark candidate as rejected
    candidate.setStatus(CandidateStatus.REJECTED);
    candidate.setRejectionReason("Domain blacklisted: " + request.getReason());
    candidateRepo.save(candidate);
}
```

### "No Funds This Year" Status

**Use Case**: Organization is valid but no current funding available

**Examples**:
```
Domain: local-foundation.bg
Reason: "Fulfilled all grant allocations for 2025, no funds available"
Revisit After: 2026-01-01
Action: MARK as "no_funds_2025"

Domain: annual-scholarship.org
Reason: "Annual scholarship awarded in March, next cycle opens January 2026"
Revisit After: 2026-01-15
Action: MARK as "no_funds_2025"
```

**Implementation**:
```java
// Human marks candidate as "no funds this year"
@PostMapping("/api/candidates/{id}/no-funds-this-year")
public void markNoFundsThisYear(@PathVariable UUID id, @RequestBody NoFundsRequest request) {
    FundingSourceCandidate candidate = candidateRepo.findById(id);
    String domain = domainExtractor.extractDomain(candidate.getSourceUrl());

    // Mark domain in Redis with revisit date
    deduplicationService.markNoFundsThisYear(
        domain,
        request.getReason(),
        request.getRevisitAfter(), // e.g., 2026-01-01
        currentUser.getEmail()
    );

    // Keep candidate in database but mark as deferred
    candidate.setStatus(CandidateStatus.DEFERRED);
    candidate.setRejectionReason("No funds available this year: " + request.getReason());
    candidateRepo.save(candidate);
}
```

---

## Redis/Valkey Configuration

### Docker Setup (Mac Studio)

```yaml
# docker-compose.yml (on Mac Studio)
version: '3.8'

services:
  valkey:
    image: valkey/valkey:latest
    container_name: northstar-valkey
    ports:
      - "6379:6379"
    volumes:
      - valkey-data:/data
    command: valkey-server --appendonly yes --maxmemory 2gb --maxmemory-policy allkeys-lru
    restart: unless-stopped

volumes:
  valkey-data:
```

### Spring Boot Configuration

```yaml
# application-dev.yml (MacBook M2)
spring:
  data:
    redis:
      host: 192.168.1.10  # Mac Studio
      port: 6379
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

## Next Steps

1. **Implement DomainDeduplicationService**
2. **Integrate with crawler workflow**
3. **Add dashboard UI for blacklisting and status management**
4. **Test deduplication with real searches**
5. **Monitor Redis cache hit rates**

---

**Document Status**: Design Complete
**Next Action**: Implement deduplication service
**Owner**: Kevin + Claude
