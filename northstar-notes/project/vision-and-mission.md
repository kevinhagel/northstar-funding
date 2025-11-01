# Vision & Mission - NorthStar Funding Discovery Platform

**Date**: 2025-10-31
**Updated**: 2025-10-31
**Tags**: #project #vision #business-model #saas #funding-discovery

---

## The Business Vision: Funding Discovery as a Service

**NorthStar Funding Discovery** is building a **subscription-based SaaS platform** that automates funding discovery for NGOs, educational institutions, and social impact organizations across Eastern Europe and beyond.

### The Market Opportunity

**The Problem**: Thousands of organizations spend 20-40 hours per week manually searching for funding opportunities across fragmented sources (Google, fundsforngos.org, candid.org, government portals, foundation websites).

**The Solution**: AI-powered platform that automatically discovers, evaluates, organizes, and updates funding opportunities, with natural language search interface.

**The Market**: Eastern Europe (Bulgaria → Balkans → EU → Global expansion)

**Revenue Model**: Subscription tiers (€50-1500/month) for NGOs, schools, municipalities, consultants

---

## The Genesis: From British Centre Burgas to Platform Vision

### Original Use Case (2023-2024)
**British Centre Burgas school** needed funding. Manual search was overwhelming. Early "Spring Crawler" project began as internal tool.

### The Insight (2024)
After exploring **fundsforngos.org** and **candid.org**, we realized:
- We could crawl and scrape the web to find funding organizations
- Parse content and convert to markdown
- Vectorize into Qdrant for semantic search
- Build microservices backend (Kafka, PostgreSQL, Redis)
- Create natural language search frontend
- **Serve paying clients, not just ourselves**

### Current Inspiration (2025)
**The NorthStar Foundation Bulgaria** school proposal validates the urgent need. If finding funding is this hard for us, it's equally hard for thousands of other organizations.

---

## The Inspiration: NorthStar Foundation Bulgaria School Proposal

The school proposal serves as our **primary test case** and **market validation**: finding funding to establish an innovative, future-ready school in Burgas, Bulgaria serving children aged 4-18 from diverse socioeconomic backgrounds.

---

## The Problem: Education and Poverty in Bulgaria

### Stark Statistics
- **30%** of Bulgarian population at risk of poverty or social exclusion (vs 21.4% EU average)
- **21.0%** of Burgas region population lives below poverty line
- **26%** of children aged 0-17 are at risk of poverty
- **National poverty line 2025**: BGN 638 (≈ EUR 326)

### The Gap
Bulgaria ranks among EU countries with highest rates of poverty and social exclusion. Children are particularly vulnerable, lacking access to quality education that could break the cycle of poverty.

---

## The Vision: Transformative Education for All

### The NorthStar Foundation Bulgaria School

**Mission**: Create a transforma tive learning environment where children are not only educated but empowered with the skills, values, and confidence to thrive in an increasingly globalised and technology-driven world.

### Core Features
- **Ages**: 4-18 (kindergarten through high school)
- **Focus**: Innovative, future-oriented education
- **Emphasis**: Foreign languages (Suggestopedia method), arts, sciences, sports
- **Special Programs**:
  - Personalized learning
  - Technology integration
  - Critical thinking and creativity
  - Entrepreneurship
  - Cultural and community engagement

### Unique Approach
- **Talent Scout Network**: Identify disadvantaged families
- **Scholarships**: Ensure access regardless of financial circumstances
- **Community Hub**: Exhibitions, musical events, sports, theatrical performances
- **Proven Foundation**: Part of 27-year established private language school, British Council partner, voted best English language school in Bulgaria for 14 consecutive years

---

## The Five Goals

### Goal 1: State-of-the-Art Educational Hub
**Objective**: Secure funding for land/building purchase, construction, and infrastructure to support innovative curriculum, performing arts, sports, and community engagement.

### Goal 2: Future-Oriented Education
**Objective**: Develop flexible curriculum with Suggestopedia language learning, diverse learning strategies, preparing students for rapidly evolving global economy.

### Goal 3: Vibrant Cultural Community Space
**Objective**: Establish dedicated spaces for artistic/cultural expression, making school a center for creativity, collaboration, social cohesion.

### Goal 4: Access for Disadvantaged Children
**Objective**: Identify families in need, provide scholarships, mentorship, tailored support led by distinguished teachers and experts.

### Goal 5: Strategic Partnerships
**Objective**: Collaborate with international educational institutions, technology companies, cultural organizations to enhance curriculum and opportunities.

---

## The Funding Discovery Challenge

### The Traditional Problem
Finding relevant funding sources is:
- **Time-consuming**: Hours of manual searching across hundreds of websites
- **Error-prone**: Missing opportunities, duplicate work
- **Overwhelming**: Thousands of funders, unclear eligibility
- **Changing**: Deadlines, programs evolve constantly

### What We Need
- Educational infrastructure grants (building/renovation)
- Program funding (curriculum development)
- Technology infrastructure grants
- Scholarships and operational support
- Capacity building grants

### Target Funders
- **EU Programs**: Horizons Europe, Erasmus+, Cohesion Funds
- **US Foundations**: America for Bulgaria Foundation, Open Society
- **Bilateral Aid**: USAID, Norway Grants, EEA Grants
- **Corporate Foundations**: Google.org, Vodafone Foundation
- **Multilateral**: World Bank, EBRD, UN agencies

---

## The Solution: NorthStar Funding Discovery

### Automated Intelligence
Rather than manually searching, let AI and automation do the heavy lifting:

1. **Discover**: Search multiple engines automatically (Searxng, Tavily, Perplexity)
2. **Judge**: AI evaluates candidates based on metadata (confidence scoring)
3. **Deduplicate**: Domain-level tracking prevents reprocessing
4. **Present**: High-confidence candidates to human reviewers
5. **Track**: Monitor sources over time, revisit when appropriate

### Human-AI Collaboration
- **AI handles**: Volume, speed, pattern recognition
- **Humans handle**: Final judgment, relationship building, contact intelligence
- **Together**: Far more effective than either alone

---

## Project Goals

### Phase 1: Foundation ✅
- Core domain model
- PostgreSQL schema
- Basic candidate workflow

### Phase 2: Automated Crawler ✅
- Nightly discovery sessions
- Domain deduplication
- High-confidence candidate creation

### Phase 3: Search Infrastructure ✅
- Three search engines (Searxng, Tavily, Perplexity)
- Virtual Threads parallel execution
- Circuit breaker protection
- 7-day query library (Monday-Sunday schedules)

### Phase 4: AI Query Generation & Metadata Judging 🚧
- LM Studio integration
- AI-powered search query generation
- Metadata-based judging (no web crawling)
- Confidence scoring

### Phase 5: RAG Search System (Future)
- Qdrant vector database
- Embedding generation (BGE-M3)
- Natural language queries
- Re-ranking with LLM

### Phase 6: Full Workflow (Future)
- Deep web crawling (Browserbase)
- Contact intelligence extraction
- Email/phone discovery
- Organization hierarchy modeling

---

## Impact Vision

### For The NorthStar Foundation Bulgaria
- Find previously unknown funding sources
- Track all relevant opportunities
- Never miss a deadline
- Build comprehensive funder knowledge base

### For Other Organizations
- Reusable platform for any funding search domain
- Adaptable to different geographies, sectors
- Open knowledge sharing

### For Burgas & Bulgaria
- Support educational infrastructure
- Empower disadvantaged communities
- Break cycles of poverty through education
- Model for other regions facing similar challenges

---

## Related Documents

- [[project-overview]] - Technical project overview
- [[architecture-overview]] - System architecture
- [[feature-003-search-infrastructure]] - Current implementation status
- Proposal: `docs/The NorthStar Foundation Bulgaria Proposal.pdf`

---

## External Resources

### The NorthStar Foundation Bulgaria
- Location: Burgas, Bulgaria
- Focus: Education for ages 4-18
- Emphasis: Vulnerable populations, future-ready skills
- Method: Scholarships, talent scouts, community engagement

### Bulgaria Education Context
- [Bulgaria Ministry of Education](https://www.mon.bg/)
- [National Science Fund](https://www.fni.bg/)
- [EU Bulgaria Programs](https://ec.europa.eu/regional_policy/en/atlas/bulgaria)

### Major Potential Funders
- [America for Bulgaria Foundation](https://us-bulgaria.org/)
- [EU Horizon Europe](https://ec.europa.eu/info/funding-tenders/)
- [Erasmus+](https://erasmus-plus.ec.europa.eu/)
- [EEA/Norway Grants](https://eeagrants.org/)

---

**Status**: Vision Document - Living Document
**Last Updated**: 2025-10-31
**Next Review**: After Feature 004 completion
