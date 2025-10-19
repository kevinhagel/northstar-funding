# RAG Architecture for NorthStar Funding Platform
## Retrieval Augmented Generation with Qdrant and LM Studio

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Design Phase

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT FRONT-END                          │
│  Natural Language Query: "Find education infrastructure      │
│  funding for our school in Burgas, Bulgaria"                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              QUERY PROCESSING (LM Studio)                    │
│  - Parse query → Extract: categories, geography, applicant   │
│  - Generate embedding for semantic search                    │
│  - Extract filters: location, amount, deadline, type         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              QDRANT VECTOR DATABASE                          │
│  - Vector similarity search on query embedding               │
│  - Apply metadata filters (geography, categories, etc.)      │
│  - Return top N matches with scores                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              RE-RANKING (LM Studio)                          │
│  - LLM analyzes results vs. original query                   │
│  - Re-rank by relevance (not just similarity)                │
│  - Generate explanations: "Why this matches your needs"      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              RESULTS RETURNED TO CLIENT                      │
│  - Funding sources with scores and explanations              │
│  - Sorted by relevance                                       │
│  - Rich details: organization, amounts, deadlines, contacts  │
└─────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

### LM Studio on Mac Studio (192.168.1.10)

**Infrastructure**:
- Mac Studio (very powerful machine)
- LM Studio running on port 1234
- Can run multiple LLMs simultaneously for different tasks

**LLM Usage by Task**:

1. **Query Understanding** (Front-end)
   - Parse natural language queries
   - Extract: categories, geography, applicant type, temporal constraints
   - Model: [TBD - fast inference model, good at entity extraction]

2. **Embedding Generation** (Vectorization)
   - Convert markdown → vector embeddings
   - Must support: English, Bulgarian, Romanian, other Eastern European languages
   - Model options:
     - Multilingual-E5 (local, multilingual, 768 dimensions)
     - BGE-M3 (multilingual, 1024 dimensions)
     - Snowflake Arctic Embed (multilingual, 768 dimensions)
     - Custom fine-tuned model (future)

3. **Re-ranking** (Search results)
   - Analyze query + results → determine true relevance
   - Generate explanations for matches
   - Model: [TBD - reasoning model, good at comparison]

4. **Query Generation for Crawler** (Discovery)
   - Generate targeted search queries for funding discovery
   - Input: Categories, geography, funding types
   - Output: Optimized search queries for Searxng/Tavily/Perplexity
   - Model: [TBD - creative, good at query formulation]

5. **Extraction & Enrichment** (Human review assistance)
   - Analyze funding source websites
   - Extract: amounts, deadlines, requirements, contact info
   - Model: [TBD - good at structured extraction]

**Benefits of LM Studio**:
- Local control (no external API costs)
- Privacy (no data sent to OpenAI/Anthropic)
- Multiple models for specialized tasks
- Fast inference on Mac Studio hardware

---

## Natural Language Query Patterns

### Example Queries Users Will Make:

```
"Find education infrastructure funding for our school in Burgas, Bulgaria"
  → Categories: [Education, Infrastructure]
  → Geography: [Bulgaria, Burgas]
  → Applicant Type: [School, Educational Institution]
  → Temporal: [Current, active deadlines]

"Show me grants over €100,000 for NGOs in Eastern Europe"
  → Amount: ≥ 100000 EUR
  → Geography: [Eastern Europe]
  → Applicant Type: [NGO]
  → Funding Type: [Grant]

"Teacher training scholarships available this year in Romania"
  → Categories: [Teacher Training, Scholarships]
  → Geography: [Romania]
  → Temporal: [This year, 2025]

"EU funding for vulnerable youth programs in the Balkans, deadline after June"
  → Categories: [Youth Development, Vulnerable Populations]
  → Geography: [Balkans]
  → Organization Type: [EU Program]
  → Temporal: [Deadline after 2025-06-01]

"Corporate foundations funding technology in schools"
  → Categories: [Education, Technology Infrastructure]
  → Organization Taxonomy: [Corporate Foundation]
  → Applicant Type: [School]
```

### Query Dimensions to Extract:

1. **Categories** (what domain/sector)
   - Education, Infrastructure, Scholarships, Youth Development, etc.

2. **Geography** (where eligible)
   - Countries: Bulgaria, Romania
   - Regions: Eastern Europe, Balkans
   - Cities: Burgas, Sofia, Bucharest

3. **Applicant Type** (who can apply)
   - NGO, School, Municipality, Religious Organization, etc.

4. **Funding Type** (mechanism)
   - Grant, Scholarship, Loan, Tender

5. **Organization Taxonomy** (who provides)
   - EU Programs, US Foundations, Corporate Foundations, etc.

6. **Temporal Constraints** (when)
   - "Available now" → deadline in future
   - "This year" → deadline in 2025
   - "After June" → deadline > 2025-06-01
   - "Urgent" → deadline within 30 days

7. **Amount Constraints** (how much)
   - "Over €100,000"
   - "Small grants" (< €10,000)
   - "Large infrastructure funding" (> €500,000)

---

## Markdown/Pre-Vectorization Format

### Design Principles

1. **Rich Context**: Include ALL information from human-AI hybrid input
2. **Hierarchical Structure**: Organization → Program → Funding Source
3. **Natural Language**: Readable by humans and LLMs
4. **Semantic Density**: Pack meaning for embedding quality
5. **Multilingual Support**: Include local language names where available

### Markdown Template

```markdown
# [Funding Source Title]

## Organization
**Name**: [Organization Name] ([Local Language Name])
**Type**: [Organization Type - e.g., Private Foundation, EU Program]
**Headquarters**: [City, Country]
**Mission**: [Organization mission statement]

[Organization description - 2-3 paragraphs about who they are, what they do]

### Focus Areas
- [Focus Area 1]
- [Focus Area 2]
- [Focus Area 3]

---

## Program
**Name**: [Program Name] ([Local Language Name if applicable])
**Description**: [Program description - what this program funds, its goals]

[2-3 paragraphs about the program's objectives, history, impact]

---

## Funding Opportunity

### Overview
[2-3 paragraph description of this specific funding opportunity, what it supports, why it exists]

### Financial Details
- **Funding Amount**: €[Min] - €[Max] EUR
- **Total Available**: €[Total Budget] EUR (if known)
- **Currency**: EUR / USD / BGN
- **Matching Required**: [Yes/No - X% if applicable]
- **Matching Type**: Cash / In-Kind / Both

### Eligibility

#### Geographic Eligibility
- **Primary**: Bulgaria, Romania, North Macedonia
- **Regions**: Eastern Europe, Balkans
- **Specific Locations**: Sofia, Burgas, Plovdiv (if city-level)

This funding is available to organizations operating in Eastern Europe, with a focus on the Balkans region. Organizations based in Bulgaria, Romania, and North Macedonia are eligible to apply. Within Bulgaria, priority is given to projects in underserved regions including Burgas and Plovdiv.

#### Organization Types Eligible
- Educational Institutions (K-12 schools, universities)
- Non-Governmental Organizations (NGOs)
- Municipalities and local government
- Faith-based organizations

This funding is open to registered non-profit organizations including schools, NGOs, and local government entities. For-profit companies are not eligible. Religious organizations may apply if the project serves the broader community.

#### Requirements
- Must be registered non-profit for at least 2 years
- Annual operating budget under €1,000,000
- Project must serve vulnerable populations
- Must have prior experience with similar projects
- Partnership with local community required

### Categories
Education, Infrastructure, Educational Infrastructure, School Buildings, Vulnerable Populations, Youth Development

This funding specifically targets educational infrastructure projects that serve vulnerable youth. Eligible activities include school building renovation, technology infrastructure for learning, and creation of safe educational spaces.

### Application Process

#### Key Dates
- **Application Opens**: 2025-02-01
- **Application Deadline**: 2025-06-30
- **Award Announcement**: 2025-09-15
- **Project Start Date**: 2025-10-01
- **Project Duration**: 12-24 months

#### How to Apply
[Detailed application instructions - 1-2 paragraphs]

**Application Portal**: https://example.org/apply

**Required Documents**:
- Project proposal (10 pages maximum)
- Budget breakdown and justification
- Organization registration documents
- Financial statements (last 2 years)
- Letters of support from community partners

#### Pre-Application
**Letter of Inquiry Required**: Yes (2 pages)
**LOI Deadline**: 2025-04-01

Organizations must submit a brief Letter of Inquiry before being invited to submit a full proposal. LOI should outline project concept, target population, and estimated budget.

### Contact Information

#### Program Officer
**Name**: [Contact Name]
**Title**: Education Program Officer
**Email**: [encrypted/not included in markdown - reference only]
**Phone**: [encrypted/not included in markdown - reference only]
**Preferred Contact**: Email

[Contact Name] is the primary program officer for education infrastructure grants. They prefer initial contact via email and typically respond within 3-5 business days. Office hours for phone consultations are Tuesdays and Thursdays, 2-4 PM EET.

#### Organization General Contact
**Email**: info@example.org
**Phone**: +1-xxx-xxx-xxxx
**Address**: [Organization address]

---

## Similar Funding Opportunities

This funder also provides:
- [Related Program 1]: [Brief description]
- [Related Program 2]: [Brief description]

---

## Historical Context

This program was established in 2010 following [context]. Since inception, it has awarded over €50 million to 200+ projects across Eastern Europe. Recent priority shifts have emphasized rural education access and technology infrastructure for remote learning.

---

## Tags
#education #infrastructure #Bulgaria #Balkans #schools #vulnerable-youth #building-renovation #technology #grants #NGO #municipality

---

## Metadata (for filtering, not embedded)
- Funding Source ID: [UUID]
- Organization ID: [UUID]
- Organization Type: PRIVATE_FOUNDATION
- Geographic Eligibility: Bulgaria, Romania, North Macedonia, Eastern Europe, Balkans
- Categories: Education, Infrastructure, Educational Infrastructure
- Applicant Types: NGO, Educational Institution, Municipality
- Amount Range: 25000-150000 EUR
- Deadline: 2025-06-30
- Status: CURRENT
- Last Verified: 2025-01-15
```

### Key Features of This Format:

1. **Hierarchical Context**: Organization → Program → Opportunity
   - LLM understands organizational context
   - "America for Bulgaria focuses on education in Bulgaria" is implicit

2. **Natural Language Dense**: Not just structured fields
   - "Priority given to underserved regions including Burgas"
   - This captures nuance that structured data misses

3. **Semantic Keywords Embedded**:
   - Categories, geography, applicant types woven into descriptions
   - Helps embedding capture multiple dimensions

4. **Contact Intelligence Referenced** (but not exposed):
   - Contact exists and is active
   - Preferences noted (email, office hours)
   - Actual email/phone encrypted in PostgreSQL, not in markdown

5. **Tags for Exact Matching**:
   - Structured tags at end
   - Can be used for metadata filtering in Qdrant

6. **Metadata Section**:
   - Not embedded (or embedded separately)
   - Used for Qdrant payload filtering
   - Enables hybrid search (semantic + exact filters)

---

## Qdrant Storage Strategy

### Collection Structure

```
Collection: funding_sources_current
  - Vector: 768 dimensions (from embedding model)
  - Payload:
    {
      "funding_source_id": "uuid",
      "organization_id": "uuid",
      "organization_name": "America for Bulgaria Foundation",
      "organization_type": "PRIVATE_FOUNDATION",
      "program_name": "Education Infrastructure Grant",
      "categories": ["Education", "Infrastructure", "Educational Infrastructure"],
      "geographic_eligibility": ["Bulgaria", "Romania", "Eastern Europe", "Balkans"],
      "applicant_types": ["NGO", "Educational Institution", "Municipality"],
      "amount_min": 25000,
      "amount_max": 150000,
      "currency": "EUR",
      "application_deadline": "2025-06-30",
      "status": "CURRENT",
      "last_verified": "2025-01-15",
      "tags": ["education", "infrastructure", "bulgaria", "schools", "vulnerable-youth"]
    }

Collection: funding_sources_historical
  - Same structure as current
  - For expired opportunities
  - Used for trend analysis, historical research
```

### Hybrid Search Example

```
User Query: "Find education infrastructure funding for our school in Burgas, Bulgaria"

1. Generate embedding for query text

2. Qdrant search with filters:
   db.search(
     collection="funding_sources_current",
     query_vector=embedding,
     query_filter={
       "must": [
         {"key": "categories", "match": {"any": ["Education", "Infrastructure"]}},
         {"key": "geographic_eligibility", "match": {"any": ["Bulgaria", "Burgas"]}},
         {"key": "applicant_types", "match": {"any": ["Educational Institution", "School"]}},
         {"key": "application_deadline", "range": {"gte": "2025-01-18"}}  // Future deadlines only
       ]
     },
     limit=20
   )

3. Returns: Top 20 results ranked by vector similarity + filter compliance
```

---

## Lifecycle Management: Current vs. Historical

### Status Transitions

```
CURRENT (Active Funding Opportunities)
  │
  ├─ application_deadline passes → AUTOMATIC → EXPIRED
  │
  ├─ Human marks as expired → MANUAL → EXPIRED
  │
  └─ Funding source updated (new deadline) → REMAINS CURRENT

EXPIRED (Past Deadlines)
  │
  └─ Moved to historical collection → HISTORICAL

HISTORICAL (Archived for Analysis)
  │
  ├─ Used for trend analysis
  ├─ Pattern recognition (what funds Bulgaria schools?)
  └─ Recurring programs: "This program opens annually in February"
```

### Automatic Expiration Process

**Nightly Job** (runs 2:00 AM):
```
1. Query all FundingSources with application_deadline < TODAY
2. Update status: CURRENT → EXPIRED
3. Re-vectorize (move from current to historical collection in Qdrant)
4. Log transition (audit trail)
```

**Manual Expiration** (human action):
```
Kevin/Huw marks funding source as expired:
  - Reason: "Program discontinued"
  - Reason: "Funder ceased operations"
  - Reason: "No longer accepting applications"
  - Update status: CURRENT → EXPIRED
  - Move to historical collection
```

### Historical Value

**Use Cases for Historical Data**:
1. **Trend Analysis**: "Show me all EU education funding from 2020-2024"
2. **Recurring Programs**: "This program opens every February - expect 2026 call"
3. **Funder Patterns**: "America for Bulgaria typically funds €50K-100K grants"
4. **Success Rate Tracking**: "If we track applications, what types get funded?"

---

## Crawler Scaling Strategy

### Problem: Nightly Crawler Overload

As database grows:
- 100 funding sources → 1 hour nightly crawl ✓
- 1,000 funding sources → 10 hours nightly crawl ✗
- 10,000 funding sources → 100 hours nightly crawl ✗✗✗

### Solution: Specialized Crawl Sessions by Type

**Weekly Rotation**:
```
Monday: EU Programs
  - Horizons Europe
  - Erasmus+
  - Cohesion Funds
  - EEA Grants
  Target: ~50 programs, 2 hours

Tuesday: US Foundations (Eastern Europe Focus)
  - America for Bulgaria
  - Open Society Foundations
  - Ford Foundation Eastern Europe programs
  Target: ~30 foundations, 1.5 hours

Wednesday: Corporate Foundations & CSR
  - Google.org
  - Vodafone Foundation
  - Microsoft Philanthropies
  Target: ~25 foundations, 1 hour

Thursday: Multilateral Organizations
  - World Bank
  - EBRD
  - UN agencies
  Target: ~20 programs, 1.5 hours

Friday: Bulgarian National Programs
  - Ministry of Education
  - National Science Fund
  - Bulgarian government grants
  Target: ~15 programs, 1 hour

Saturday: Regional/Local Programs
  - Sofia Municipality
  - Other Bulgarian regions
  Target: ~10 programs, 0.5 hours

Sunday: Aggregator Sites & New Discoveries
  - grants.gov (check for new Bulgaria-relevant)
  - fundsforngos.org
  - Run exploratory queries for new funders
  Target: Variable, 2 hours
```

### Dynamic Scheduling

**Priority-Based**:
- High priority: Programs with deadlines in next 30 days → Weekly check
- Medium priority: Programs with deadlines in 30-90 days → Bi-weekly check
- Low priority: Programs with deadlines > 90 days → Monthly check
- Historical: Expired programs → No crawling, archive only

**DiscoverySession Enhancements**:
```java
@Table("discovery_session")
public class DiscoverySession {
    // ... existing fields ...

    // Specialized crawl type
    private CrawlType crawlType; // EU_PROGRAMS, US_FOUNDATIONS, CORPORATE, etc.
    private ScheduleFrequency frequency; // NIGHTLY, WEEKLY, MONTHLY
    private Set<UUID> targetOrganizationIds; // Specific orgs to crawl this session
}

public enum CrawlType {
    EU_PROGRAMS,
    US_FOUNDATIONS,
    CORPORATE_FOUNDATIONS,
    MULTILATERAL_ORGS,
    NATIONAL_GOVERNMENT,
    REGIONAL_LOCAL,
    AGGREGATORS,
    EXPLORATORY  // New discovery
}

public enum ScheduleFrequency {
    NIGHTLY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    MANUAL  // Human-triggered
}
```

---

## Embedding Model Selection

### Requirements

1. **Multilingual Support**:
   - English (primary for international funders)
   - Bulgarian (local language, local names)
   - Romanian, Albanian, Serbian (neighboring countries)

2. **Semantic Understanding**:
   - "School" = "Educational Institution" (synonyms)
   - "Balkans" includes "Bulgaria" (geographic hierarchy)
   - "Infrastructure" + "Education" = "Educational Infrastructure" (composition)

3. **Performance**:
   - Fast inference on Mac Studio
   - Reasonable vector dimensions (768-1024 ideal)

4. **Local Deployment**:
   - Can run in LM Studio
   - No external API dependency

### Candidate Models

**Option 1: BGE-M3** (Recommended)
- Multilingual (100+ languages)
- 1024 dimensions
- State-of-the-art retrieval performance
- Runs on LM Studio
- Supports English, Bulgarian, Romanian, etc.

**Option 2: Multilingual-E5**
- Multilingual (100+ languages)
- 768 dimensions
- Good balance of performance and size
- Microsoft model, well-documented

**Option 3: Snowflake Arctic Embed**
- Multilingual
- 768 dimensions
- Open source, good performance
- Recently released (2024)

### Testing Plan

1. Create small test set (20-30 funding sources)
2. Generate embeddings with each model
3. Run test queries
4. Measure retrieval quality (precision, recall)
5. Choose best performing model

---

## Re-Vectorization Strategy

### When to Re-Vectorize?

**Trigger Events**:

1. **FundingSource Updated** (HIGH PRIORITY)
   - Amount changed
   - Deadline extended
   - Requirements updated
   - Geographic eligibility changed
   → Re-generate markdown → Re-embed → Update Qdrant

2. **Organization Updated** (MEDIUM PRIORITY)
   - Mission statement changed
   - Focus areas updated
   - Name changed
   → Re-vectorize ALL funding sources from this org

3. **Program Updated** (MEDIUM PRIORITY)
   - Program description changed
   - Eligibility updated
   → Re-vectorize ALL funding sources in this program

4. **Embedding Model Upgraded** (LOW PRIORITY)
   - New model version released
   - Better multilingual model available
   → Batch re-vectorize ALL funding sources (scheduled job)

### Re-Vectorization Process

```java
public class VectorizationService {

    public void reVectorize(FundingSource fundingSource) {
        // 1. Generate fresh markdown
        String markdown = markdownGenerator.generate(fundingSource);

        // 2. Generate new embedding
        float[] embedding = embeddingModel.embed(markdown);

        // 3. Update or create in Qdrant
        if (fundingSource.getQdrantPointId() != null) {
            // Update existing point
            qdrantClient.update(
                collection = determineCollection(fundingSource.getStatus()),
                pointId = fundingSource.getQdrantPointId(),
                vector = embedding,
                payload = buildPayload(fundingSource)
            );
        } else {
            // Create new point
            UUID pointId = qdrantClient.upsert(
                collection = "funding_sources_current",
                vector = embedding,
                payload = buildPayload(fundingSource)
            );
            fundingSource.setQdrantPointId(pointId);
        }

        // 4. Update FundingSource metadata
        fundingSource.setVectorizedAt(LocalDateTime.now());
        fundingSource.setMarkdownRepresentation(markdown);
        fundingSource.setVectorEmbeddingVersion("v1.0");

        // 5. Save to PostgreSQL
        fundingSourceRepository.save(fundingSource);
    }

    private String determineCollection(FundingSourceStatus status) {
        return status == FundingSourceStatus.CURRENT
            ? "funding_sources_current"
            : "funding_sources_historical";
    }
}
```

---

## Performance Considerations

### Vector Dimensions

- **768 dimensions**: ~3 KB per vector (recommended)
- **1024 dimensions**: ~4 KB per vector
- **1536 dimensions**: ~6 KB per vector (OpenAI ada-002)

With 10,000 funding sources:
- 768-dim: ~30 MB vector storage
- 1024-dim: ~40 MB vector storage
- Very manageable

### Search Latency

- Qdrant vector search: < 50ms for 10K vectors
- Embedding generation: ~100-500ms (depends on model)
- Re-ranking with LLM: ~500-2000ms (depends on model)
- **Total latency**: ~1-3 seconds (acceptable for user search)

### Batch Vectorization

When adding 50 new funding sources from nightly crawl:
- Generate markdown: ~1 second (50 sources)
- Generate embeddings: ~5-25 seconds (depends on model)
- Upsert to Qdrant: ~1 second
- **Total**: ~7-30 seconds (acceptable)

---

## Next Steps

1. **Model Selection**:
   - [ ] Test BGE-M3, Multilingual-E5, Arctic Embed
   - [ ] Benchmark on sample funding sources
   - [ ] Choose embedding model

2. **Markdown Template**:
   - [ ] Finalize template structure
   - [ ] Implement MarkdownGenerator service
   - [ ] Test with real funding sources

3. **Qdrant Setup**:
   - [ ] Install Qdrant on Mac Studio (or MacBook for dev)
   - [ ] Create collections (current, historical)
   - [ ] Test insert/search operations

4. **LM Studio Configuration**:
   - [ ] Select query understanding model
   - [ ] Select re-ranking model
   - [ ] Configure for concurrent requests

5. **Integration**:
   - [ ] Implement VectorizationService
   - [ ] Create background job for automatic vectorization
   - [ ] Build search API endpoint

---

## Open Questions

1. **Markdown vs. JSON**: Is markdown optimal, or should we use structured JSON for embeddings?
2. **Multiple Embeddings**: Should we embed organization, program, funding source separately and combine?
3. **Metadata Filtering**: Which filters are most important (prioritize for indexing)?
4. **Re-ranking Model**: Do we need re-ranking, or is vector search + filters sufficient?
5. **Multilingual Queries**: Should we detect query language and use language-specific prompts?

---

**Document Status**: Design Phase - Awaiting Feedback
**Next Action**: Test embedding models with sample data
**Owner**: Kevin + Claude
