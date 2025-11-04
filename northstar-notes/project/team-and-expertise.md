# Team & Expertise - NorthStar Funding Discovery Project

**Date**: 2025-11-04
**Purpose**: Document team members, their expertise, and how it shapes the platform
**Context**: Understanding who we're building for and who we're building with

---

## Core Team

### Kevin Hagel - Technical Lead & Platform Architect
**Role**: Lead developer, back-end architect
**Expertise**:
- 40 years software development experience
- Java expert
- Spring Framework expert
- Back-end systems, databases, distributed systems
- **Not a front-end developer** - focuses on services, APIs, data models

**Platform Contribution**:
- System architecture (multi-module Maven, Spring Boot)
- Domain model design (19 entities, 11 enums)
- Persistence layer (Spring Data JDBC, PostgreSQL)
- Search infrastructure (4 search providers, anti-spam filtering)
- AI integration (LangChain4j, LM Studio)
- Query generation service (strategy pattern, Caffeine caching)

**Personal Context**:
- American expat living in Burgas, Bulgaria
- Husband to Zlatina Petkova (British Centre Burgas owner)
- Inspired to build this platform to help Zlatina and others like her find funding

---

### Zlatina Petkova - Founder, British Centre Burgas (Primary User Persona)
**Role**: Educational entrepreneur, platform inspiration
**Expertise**:
- 28 years running British Centre Burgas (since 1997)
- English language education (ages 4-18)
- Cambridge/Pearson examination center management
- Voted best English language school in Bulgaria for 14 consecutive years
- Deep understanding of private education in Bulgaria

**Platform Need**:
- Finding funding to purchase a building for her ideal school environment
- For-profit education funding (often excluded from traditional grants)
- Bulgarian language search interface needed
- Real-world test case for platform design

**Inspiration**: "She wants to buy her own building so she can have exactly the kind of school she wants, for her kind of students. She is my inspiration."

---

### Gallie Simeonova - Front-End Designer
**Role**: UI/UX design, front-end development
**Expertise**:
- Expert front-end designer
- User interface design
- User experience optimization
- Visual design and branding

**Platform Contribution** (Future):
- Search interface design (Bulgarian + English)
- Results presentation and matching score visualization
- User profile and organization management UI
- Multilingual UI support
- Responsive design for schools/NGOs

**Note**: Front-end development is Phase 5+ (after back-end services complete)

---

### Huw Jones - Grant Funding Expert
**Role**: Grant expertise advisor, future platform user
**Expertise**:
- Expert in obtaining grants and funding
- 15+ years experience with rural telemedicine funding in Africa
- Founder/Director: Virtual Doctors (https://virtualdoctors.org/)
- Deep knowledge of:
  - Grant application processes
  - Funding provider relationships
  - International development funding
  - Healthcare/education funding landscape
  - Rural and underserved population funding
- UK-based, works in Bulgaria (teaches at British Centre Burgas)

**Platform Value**:
- **Domain expertise validation**: Huw can validate our taxonomy, search categories, and matching logic
- **Real-world feedback**: He knows what grant seekers actually need
- **Future use case**: Virtual Doctors platform (healthcare funding discovery for rural Africa)
- **Network**: Connections to funding providers and grant-making organizations

**Kevin's Note**: "Huw is an expert with grants, grant providers, he has expertise in areas I can't even describe since I don't know how this stuff works :-)"

**Future Vision**: Build a similar funding discovery platform for Virtual Doctors (healthcare/telemedicine funding in Africa)

---

### Proletina Jones - Legal Expert
**Role**: Legal advisor, compliance consultant
**Expertise**:
- Lawyer specializing in UK law
- Expert in Bulgarian law
- Cross-border legal issues
- NGO and foundation legal structures
- Compliance and regulatory requirements

**Platform Contribution** (Future):
- Legal structure advice for NorthStar Foundation Bulgaria (NGO)
- Compliance requirements for funding applications
- Data privacy (GDPR) compliance
- Terms of service and user agreements for SaaS platform
- International funding legal requirements

**Context**: Married to Huw Jones, part of the British Centre Burgas community

---

### Petko Petkov - NorthStar Foundation Co-Founder
**Role**: NorthStar Foundation Bulgaria co-founder
**Background**:
- Son of Zlatina Petkova
- Works at British Centre Burgas
- Co-founder of NorthStar Foundation Bulgaria (NGO)

**Platform Context**:
- Part of the team that conceived NorthStar Foundation
- Represents next generation of educational leadership
- Future platform user (NGO perspective)

**NorthStar Foundation**: Co-founded with Zlatina, Huw, Proletina, and Kevin to provide scholarships and grant assistance to educational organizations

---

## The NorthStar Foundation Bulgaria (NGO)

### Organization Details
**Type**: NGO (nonprofit organization)
**Mission**:
1. Provide scholarships for disadvantaged children
2. Assist educational organizations with grant applications
3. Support access to quality education in Bulgaria

**Founders**: Zlatina Petkova, Petko Petkov, Huw Jones, Proletina Jones, Kevin Hagel

**Dual Role**:
1. **Platform user**: Will use NorthStar Funding Discovery to find funding for the foundation's school project
2. **Service provider**: Will offer grant application assistance services to other organizations using the platform

**Funding Needs** (as platform user):
- Land/building purchase and construction (€500k-2M)
- Scholarship program for disadvantaged children (26% of Bulgarian children at poverty risk)
- Curriculum development (Suggestopedia, future-ready skills)
- Technology infrastructure
- Cultural/community hub facilities
- Teacher training and capacity building

---

## How Team Expertise Shapes the Platform

### 1. Real-World Use Cases Drive Design

**Zlatina's Need** → For-profit education support
- Add FOR_PROFIT_EDUCATION_PROVIDER to RecipientOrganizationType
- Include loans and private investment in FundingMechanism
- Search for building purchase funding (MEDIUM/LARGE ProjectScale)

**Huw's Expertise** → Grant application workflow
- Future: Grant application assistance features
- Validate taxonomy against real-world funding categories
- Ensure search results include eligibility criteria and application processes

**NorthStar Foundation** → NGO multi-dimensional needs
- Multiple funding mechanisms (grants, matching grants, scholarships)
- Multiple categories (infrastructure, programs, scholarships, technology)
- Multiple beneficiary populations (disadvantaged children, low-income families)

### 2. Bulgarian Context Informs Geographic Focus

**Primary Market**: Bulgaria → Eastern Europe → EU → Global
- Bulgarian language support is CRITICAL (not optional)
- GeographicScope includes: BULGARIA, EASTERN_EUROPE, BALKANS, EU_MEMBER_STATES
- In-country knowledge: Zlatina (28 years), Proletina (legal), Huw (grants)

### 3. Expertise Gaps Inform Technology Choices

**Kevin**: Back-end expert, NOT front-end
- Build robust services and APIs first (Phases 1-4)
- Gallie handles front-end design (Phase 5+)
- API-first architecture allows independent front-end development

**Kevin**: NOT a grant expert
- Huw validates domain model and taxonomy
- Real-world feedback from grant seekers (Zlatina, NorthStar Foundation)
- Learn from users rather than assume requirements

### 4. Future Platform Expansion

**Virtual Doctors** (Huw's organization):
- Healthcare/telemedicine funding discovery for rural Africa
- Different taxonomy (healthcare vs education)
- Different geography (sub-Saharan Africa)
- Same core platform architecture (search-crawler-scraper-match)

**Reusable Platform Pattern**:
- NorthStar Funding Discovery = education funding, Eastern Europe
- Future: Virtual Doctors Funding Discovery = healthcare funding, Africa
- Same technology stack, different domain taxonomies

---

## Team Roles in Feature Development

### Feature 005: Enhanced Taxonomy

**Kevin** (Lead Developer):
- Implement 5 new enums (FundingSourceType, FundingMechanism, etc.)
- Update QueryGenerationRequest for multi-dimensional queries
- Implement weekly scheduling logic
- Write tests for new taxonomy

**Huw** (Grant Expert - Consultant):
- Validate that taxonomy matches real-world funding landscape
- Review FundingSourceType values (are these the right categories?)
- Review BeneficiaryPopulation (are we missing key populations?)
- Confirm RecipientOrganizationType includes all relevant organization types

**Zlatina** (User Persona - Tester):
- Test Bulgarian language query generation (when implemented)
- Validate that for-profit education funding sources are discoverable
- Provide feedback on search results relevance

**NorthStar Foundation** (User Persona - Tester):
- Test multi-dimensional queries (infrastructure + scholarships + technology)
- Validate that NGO-focused funding sources are discoverable
- Confirm that beneficiary population targeting works (disadvantaged children)

**Gallie** (Front-End - Future Phase):
- Design UI for multi-dimensional search filters
- Create visualization for match scores
- Bulgarian + English language UI

**Proletina** (Legal - Future Phase):
- Review data privacy and GDPR compliance
- Advise on terms of service for SaaS platform
- Ensure funding source data collection is compliant

---

## Communication & Collaboration Patterns

### Kevin ↔ Zlatina
- **Frequency**: Daily (married, living together)
- **Context**: Real-time user feedback, requirements validation
- **Language**: English (Kevin), Bulgarian (Zlatina) - bilingual household

### Kevin ↔ Huw
- **Frequency**: Regular (colleagues at British Centre Burgas)
- **Context**: Grant expertise validation, Virtual Doctors future planning
- **Language**: English

### Kevin ↔ Gallie
- **Frequency**: As needed (front-end development not started yet)
- **Context**: Future UI/UX design collaboration
- **Language**: English + Bulgarian

### Kevin ↔ NorthStar Foundation Team
- **Frequency**: Regular (Zlatina, Petko, Huw, Proletina)
- **Context**: Foundation planning, platform requirements, testing
- **Language**: English + Bulgarian

---

## Team Strengths & Complementary Skills

### Technical Expertise
✅ **Back-End Development**: Kevin (40 years, Java/Spring expert)
✅ **Front-End Development**: Gallie (expert designer)
✅ **Legal/Compliance**: Proletina (UK + Bulgarian law)

### Domain Expertise
✅ **Grant Funding**: Huw (15+ years, international development)
✅ **Private Education**: Zlatina (28 years, British Centre Burgas)
✅ **NGO Operations**: NorthStar Foundation team

### Geographic/Cultural Knowledge
✅ **Bulgaria**: Zlatina (native), Proletina (native/lawyer), Petko (native)
✅ **UK**: Huw (native), Proletina (lawyer)
✅ **USA**: Kevin (native, 40 years tech experience)
✅ **Africa**: Huw (Virtual Doctors, rural telemedicine)

### Language Skills
✅ **Bulgarian**: Zlatina, Petko, Proletina, Gallie (native speakers)
✅ **English**: Kevin, Huw (native speakers), Zlatina (expert teacher)
✅ **Bilingual**: Zlatina, Proletina, Gallie, Petko

---

## What This Means for Platform Design

### 1. User Interface Must Be Bilingual (Minimum)
- Bulgarian is NOT optional - it's a PRIMARY language
- Search queries in Bulgarian → generate searches in Bulgarian + English + German
- Results must be presentable in Bulgarian

### 2. For-Profit Education Is a First-Class Use Case
- Not an afterthought or edge case
- Zlatina's building purchase is THE driving use case
- Taxonomy must support for-profit funding sources

### 3. Grant Expert Validation Is Available
- Huw can review taxonomy and search strategies
- Real-world feedback from someone who actually secures grants
- Don't guess - ask Huw

### 4. Legal Compliance Is Built-In
- Proletina can advise on GDPR, data privacy, terms of service
- UK + Bulgarian legal expertise in-house
- International funding legal requirements

### 5. Front-End Design Will Be Professional
- Gallie's expertise ensures high-quality UI/UX
- Kevin focuses on back-end (plays to strengths)
- API-first architecture supports independent development

### 6. Platform Is Reusable for Other Domains
- Virtual Doctors (healthcare) is natural next application
- Same architecture, different taxonomy
- Multi-domain SaaS platform potential

---

## Team Diversity as Competitive Advantage

### Traditional Grant Aggregators:
- US-centric (Grants.gov, Candid)
- English-only interfaces
- Focus on large, established funders
- No for-profit education support

### NorthStar Funding Discovery Team:
✅ **Geographic diversity**: USA, UK, Bulgaria, Africa connections
✅ **Language diversity**: English, Bulgarian, multilingual capability
✅ **Sector diversity**: Education (private + NGO), healthcare, telemedicine
✅ **Expertise diversity**: Tech (Kevin), Design (Gallie), Legal (Proletina), Grants (Huw), Education (Zlatina)
✅ **Cultural knowledge**: Deep understanding of Eastern European funding landscape

**Result**: Platform designed for underserved markets (Eastern Europe, for-profit education, rural/disadvantaged populations)

---

## Future Team Needs (As Platform Grows)

### Phase 5-6 (RAG Search, Deep Crawling):
- **Need**: Back-end developers (Kevin can't do everything)
- **Skills**: Spring Boot, PostgreSQL, vector databases (Qdrant)

### Phase 5 (Front-End Development):
- **Have**: Gallie (expert designer)
- **May Need**: Additional front-end developers for React/Vue.js implementation

### Phase 7+ (SaaS Operations):
- **Need**: DevOps engineer (deployment, monitoring, scaling)
- **Need**: Customer success manager (onboard clients, support)
- **Need**: Sales/marketing (reach Bulgarian/Romanian/Polish schools and NGOs)

### Phase 8+ (Country Expansion):
- **Need**: Native speakers for Romanian, Polish, Greek markets
- **Need**: In-country grant experts (like Huw but for each country)

---

## Related Documents

- [[vision-and-mission]] - Platform vision and NorthStar Foundation mission
- [[future-roadmap-extensibility]] - Development phases and expansion plans
- [[funding-industry-terminology]] - Domain language and industry terms

---

**Last Updated**: 2025-11-04
**Status**: Team documentation
**Next Review**: As team grows or roles change
