# TLD Credibility Research for Funding Source Detection

**Date**: 2025-11-05
**Purpose**: Establish domain credibility scoring for automated funding source detection
**Context**: Story 1.3 - Search Result Processing

---

## Executive Summary

Domain TLD credibility is more nuanced than simple `.gov/.edu/.org` classification. Our research reveals:

1. **Validated nonprofit TLDs** (`.ngo`, `.foundation`) offer higher trust than generic `.org`
2. **Country-code TLDs** (ccTLDs) are highly relevant for regional funding (`.bg`, `.eu`, `.ro`)
3. **Government second-level domains** (`.gov.bg`, `.eeagrants.cz`) indicate official sources
4. **Free/cheap TLDs** (`.xyz`, `.top`, `.tk`, `.ml`) are heavily associated with spam
5. **European Union domains** (`.europa.eu`) are reserved for EU institutions

---

## TLD Classification Matrix

### TIER 1: Highest Credibility (Score: +0.20)

**Validated Nonprofit TLDs:**
- `.ngo` / `.ong` - Require legal validation of NGO status, closed domain preventing fraud
- `.foundation` - Validated philanthropic organizations, foundations, trusts
- `.charity` - Validated charitable organizations

**Government Official:**
- `.gov` (US government only)
- `.gov.[country]` (e.g., `.gov.bg`, `.gov.ro`, `.gov.pl`)
- `.eeagrants.org` - EEA and Norway Grants (validated)
- `.europa.eu` - Reserved for EU institutions and agencies

**Academic/Education:**
- `.edu` (US accredited institutions)
- `.edu.[country]` / `.ac.[country]` (e.g., `.edu.bg`, `.ac.bg`)

**Why Tier 1**: These TLDs require validation, legal status verification, or government authorization. Cannot be freely registered.

---

### TIER 2: High Credibility (Score: +0.15)

**Established Nonprofit:**
- `.org` - Traditional nonprofit TLD (but not validated, so lower than `.ngo`)

**EU Domains:**
- `.eu` - 3.72M domains, 9th largest ccTLD, restricted to EU citizens/organizations
- `.ею` - Cyrillic variant of `.eu`

**Target Region ccTLDs:**
- `.bg` - Bulgaria (operated by Register.BG, EU-restricted)
- `.ro` - Romania
- `.pl` - Poland (~2.5M domains)
- `.cz` - Czech Republic (1M+ domains)
- `.de` - Germany
- `.fr` - France
- `.бг` - Bulgaria Cyrillic IDN ccTLD (validated)

**Funding-Specific TLDs:**
- `.fund` - Fundraising/grant giving organizations
- `.gives` - Philanthropic organizations

**Why Tier 2**: These TLDs have geographic/organizational restrictions but don't require full validation like `.ngo`. ccTLDs for Eastern Europe are highly relevant to your target region.

---

### TIER 3: Medium Credibility (Score: +0.08)

**Generic Business TLDs:**
- `.com` - 40% of phishing attacks BUT also most legitimate organizations
- `.net` - Traditional business/network TLD
- `.info` - Information sites

**Other European ccTLDs:**
- `.gr` - Greece
- `.hu` - Hungary
- `.at` - Austria
- `.it` - Italy
- `.es` - Spain

**Educational TLDs:**
- `.education` - Educational institutions (not validated)

**Why Tier 3**: Widely available, minimal restrictions, mixed usage. Require additional signals (keywords, content) to determine credibility.

---

### TIER 4: Low Credibility (Score: 0.00)

**Cheap/Unrestricted TLDs:**
- `.biz` - Business (cheap registration)
- `.co` - Commercial
- `.io` - Tech startups (originally British Indian Ocean Territory)
- `.me` - Montenegro ccTLD (widely marketed globally)

**Why Tier 4**: Neutral score. No inherent credibility or spam association. Require strong additional signals.

---

### TIER 5: NEGATIVE Credibility (Score: -0.15 to -0.30)

**Free Domain TLDs (Highest Spam Risk):**
- `.tk` - Tokelau (Freenom free domains) - **-0.30**
- `.ml` - Mali (Freenom free domains) - **-0.30**
- `.ga` - Gabon (Freenom free domains) - **-0.30**
- `.cf` - Central African Republic (Freenom free domains) - **-0.30**
- `.gq` - Equatorial Guinea (Freenom free domains) - **-0.30**

**Cheap Spam-Heavy TLDs:**
- `.xyz` - 2nd most phishing domains after `.com` - **-0.20**
- `.top` - Heavily used for phishing - **-0.20**
- `.icu` - $2 registration, phishing favorite - **-0.20**
- `.loan` - High malicious activity volume - **-0.25**
- `.click` - Suspicious activity - **-0.15**
- `.buzz` - Worst for phishing (per research) - **-0.20**
- `.cam` - Worst for spam - **-0.15**
- `.pw` - Phishing actor preference - **-0.15**
- `.shop` - Entered top 10 suspicious TLDs 2022 - **-0.10**

**Why Tier 5**: These TLDs are heavily abused for phishing, spam, and scams. Free/cheap registration attracts bad actors. Finding legitimate funding sources on these TLDs is extremely rare.

---

## Real-World Examples Found

### Bulgaria Funding Sources:
- **fulbright.bg** - Fulbright Bulgaria (.bg ccTLD) ✅ Tier 2
- **su.bg** - Sofia University scholarships (.bg ccTLD) ✅ Tier 2
- **bg-embassy.org** - Embassy portal (.org) ✅ Tier 2

### EU Funding Sources:
- **eeagrants.cz** - EEA Grants Czech Republic (.cz ccTLD) ✅ Tier 2
- **oportunitati-ue.gov.ro** - Romania EU Funding (.gov.ro) ✅ Tier 1
- **eeagrants.org** - EEA and Norway Grants (.org) ✅ Tier 2
- **european-union.europa.eu** - EU official site (.europa.eu) ✅ Tier 1

### Eastern Europe Foundations:
- **eef.md** - East Europe Foundation Moldova (.md ccTLD) ✅ Tier 2

---

## Implications for Confidence Scoring

### Updated Algorithm Proposal:

```
Base confidence: 0.00

1. TLD Credibility Score (see matrix above): -0.30 to +0.20
2. Funding keywords in title: +0.15
3. Funding keywords in description: +0.10
4. Geographic relevance (Bulgaria/EU/Eastern Europe): +0.15
5. Organization type detected (Ministry/Commission/Foundation): +0.15
6. Multiple positive signals (compound boost): +0.15

Minimum score: 0.00 (spam TLDs capped at 0.00 unless exceptional keywords)
Maximum score: 1.00

Threshold for PENDING_CRAWL: >= 0.60
```

### Example Calculations:

**Example 1: fulbright.bg**
- TLD `.bg` (Tier 2): +0.15
- Keywords "scholarship" in title: +0.15
- Geographic "Bulgaria" match: +0.15
- Organization type "Fulbright": +0.15
- Multiple signals boost: +0.15
- **Total: 0.75** → PENDING_CRAWL ✅

**Example 2: random-charity.xyz**
- TLD `.xyz` (Tier 5): -0.20 → capped at 0.00
- Keywords "charity" in title: +0.15
- No geographic match: 0.00
- No organization type: 0.00
- **Total: 0.15** → LOW_CONFIDENCE ❌

**Example 3: european-union.europa.eu**
- TLD `.europa.eu` (Tier 1): +0.20
- Keywords "funding" in description: +0.10
- Geographic "European" match: +0.15
- Organization "European Commission": +0.15
- Multiple signals boost: +0.15
- **Total: 0.75** → PENDING_CRAWL ✅

---

## Implementation Recommendations

### 1. Create DomainCredibilityService

**Purpose**: Centralized TLD scoring logic with comprehensive test coverage

**Key Methods**:
```java
public BigDecimal getTldScore(String domain)
public TldTier getTldTier(String domain)
public boolean isSpamTld(String domain)
public boolean isValidatedNonprofit(String domain)
public boolean isGovernmentDomain(String domain)
```

**Test Coverage**:
- Test all 5 tiers with representative TLDs
- Test second-level domains (`.gov.bg`, `.edu.bg`)
- Test edge cases (subdomains, ports, paths)
- Test IDN ccTLDs (`.бг`, `.ею`)
- Test case insensitivity
- Verify BigDecimal scale 2 precision

---

### 2. Blacklist Spam TLDs Early

**Strategy**: Filter Tier 5 TLDs during search result processing BEFORE domain deduplication

**Rationale**: No point tracking/deduplicating spam domains. Save processing resources.

**Exception**: If exceptional keywords present (e.g., "European Commission"), allow through but cap TLD score at 0.00

---

### 3. Geographic Boost for Regional ccTLDs

**Enhancement**: Apply additional +0.05 boost for target region ccTLDs when combined with funding keywords

**Target ccTLDs**: `.bg`, `.ro`, `.pl`, `.cz`, `.de`, `.fr`, `.eu`, `.бг`

**Example**: A `.bg` domain with funding keywords gets:
- Base TLD score: +0.15
- Geographic boost: +0.05
- **Total TLD contribution: +0.20** (matching Tier 1!)

---

### 4. Validated Nonprofit Detection

**Pattern**: Domains on `.ngo`, `.foundation`, `.charity` TLDs should receive highest confidence

**Rationale**: These require validation, indicating legitimate nonprofit status

**Risk**: Very rare in search results, but when found, likely high-quality candidates

---

## Future Enhancements

### Phase 2 (Deep Crawling):
- Verify claimed TLD credibility against actual website content
- Detect mismatches (e.g., `.foundation` TLD but no foundation information on site)
- Build reputation scores per domain over time

### Phase 3 (Machine Learning):
- Train model on approved/rejected candidates
- Learn which TLD patterns correlate with approval rates
- Detect emerging spam TLDs automatically

---

## References

**Research Sources**:
1. Empower Agency - Charity Domain Names TLD Guide
2. EuroDNS - Domain Extensions for Governments & Non-Profits
3. Wikipedia - .ngo, .eu, .bg, ccTLD articles
4. Cybercrime Information Center - Top 20 Malicious Phishing TLDs
5. SOCRadar - Top 10 TLDs Threat Actors Use for Phishing
6. Cloudflare Blog - TLD Email Phishing Threats
7. Palo Alto Unit 42 - TLDs and Cybercrime

**Data Updated**: 2025-11-05

---

## Conclusion

Our research reveals that **TLD credibility is a multi-dimensional spectrum**, not a binary classification. Key insights:

1. **Validated nonprofit TLDs** (`.ngo`, `.foundation`) are more trustworthy than generic `.org`
2. **Regional ccTLDs** (`.bg`, `.eu`, `.ro`) are highly relevant for Eastern Europe funding
3. **Government domains** (`.gov.bg`, `.europa.eu`) indicate official sources
4. **Free/cheap TLDs** (`.xyz`, `.top`, Freenom domains) are spam havens
5. **Context matters** - A `.com` domain with strong keywords/organization type can outscore a bare `.org` domain

**Next Step**: Implement `DomainCredibilityService` with comprehensive test suite covering all tiers and edge cases.
