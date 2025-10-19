# Geographic Hierarchy for Funding Sources
## Multi-Level Geographic Classification System

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Approved

---

## Design Goals

1. **Support Multiple Granularities**: From continents to cities
2. **Handle Political and Economic Blocs**: EU, NATO, ASEAN, etc.
3. **Support Geographic Regions**: "Eastern Europe", "Balkans", "East Asia"
4. **Enable Flexible Queries**: "Show funding for EU members" or "Show funding for Eastern Europe"
5. **International Standards**: Use ISO 3166 for countries where applicable
6. **Historical Tracking**: Track membership changes (e.g., UK leaving EU)

---

## Complete Hierarchy

```
GLOBAL
│
├─── NORTH AMERICA (Continent)
│    ├─── United States
│    ├─── Canada
│    ├─── Mexico
│    └─── Central America (Sub-region)
│         ├─── Guatemala
│         ├─── Honduras
│         ├─── Costa Rica
│         ├─── Panama
│         ├─── El Salvador
│         ├─── Nicaragua
│         └─── Belize
│
├─── SOUTH AMERICA (Continent)
│    ├─── Andean Region (Sub-region)
│    │    ├─── Colombia
│    │    ├─── Ecuador
│    │    ├─── Peru
│    │    ├─── Bolivia
│    │    └─── Venezuela
│    ├─── Southern Cone (Sub-region)
│    │    ├─── Argentina
│    │    ├─── Chile
│    │    ├─── Uruguay
│    │    └─── Paraguay
│    └─── Tropical South America (Sub-region)
│         ├─── Brazil
│         ├─── Guyana
│         ├─── Suriname
│         └─── French Guiana
│
├─── EUROPE (Continent)
│    │
│    ├─── EUROPEAN UNION (Political Bloc)
│    │    ├─── Austria (member)
│    │    ├─── Belgium (member)
│    │    ├─── Bulgaria (member since 2007)
│    │    ├─── Croatia (member)
│    │    ├─── Cyprus (member)
│    │    ├─── Czech Republic (member)
│    │    ├─── Denmark (member)
│    │    ├─── Estonia (member)
│    │    ├─── Finland (member)
│    │    ├─── France (member)
│    │    ├─── Germany (member)
│    │    ├─── Greece (member)
│    │    ├─── Hungary (member)
│    │    ├─── Ireland (member)
│    │    ├─── Italy (member)
│    │    ├─── Latvia (member)
│    │    ├─── Lithuania (member)
│    │    ├─── Luxembourg (member)
│    │    ├─── Malta (member)
│    │    ├─── Netherlands (member)
│    │    ├─── Poland (member)
│    │    ├─── Portugal (member)
│    │    ├─── Romania (member since 2007)
│    │    ├─── Slovakia (member)
│    │    ├─── Slovenia (member)
│    │    ├─── Spain (member)
│    │    └─── Sweden (member)
│    │
│    ├─── EUROZONE (Economic Bloc - subset of EU, uses EUR currency)
│    │    ├─── Austria (EUR)
│    │    ├─── Belgium (EUR)
│    │    ├─── France (EUR)
│    │    ├─── Germany (EUR)
│    │    ├─── Greece (EUR)
│    │    ├─── Ireland (EUR)
│    │    ├─── Italy (EUR)
│    │    ├─── Portugal (EUR)
│    │    ├─── Spain (EUR)
│    │    └─── [19 other EUR-using countries]
│    │
│    │    Note: Bulgaria, Romania, Poland use own currencies (BGN, RON, PLN)
│    │
│    ├─── NATO (Political/Military Bloc)
│    │    ├─── [30 member countries]
│    │    └─── Bulgaria (member since 2004)
│    │
│    ├─── EASTERN EUROPE (Geographic Region)
│    │    ├─── Balkans (Sub-region)
│    │    │    ├─── Albania (ISO: AL)
│    │    │    ├─── Bosnia and Herzegovina (ISO: BA)
│    │    │    ├─── Bulgaria (ISO: BG)
│    │    │    │    ├─── Sofia Province
│    │    │    │    │    └─── Sofia (city)
│    │    │    │    ├─── Plovdiv Province
│    │    │    │    │    └─── Plovdiv (city)
│    │    │    │    ├─── Varna Province
│    │    │    │    ├─── Burgas Province
│    │    │    │    └─── [24 other provinces]
│    │    │    ├─── Greece (ISO: GR)
│    │    │    ├─── Kosovo (ISO: XK)
│    │    │    ├─── Montenegro (ISO: ME)
│    │    │    ├─── North Macedonia (ISO: MK)
│    │    │    ├─── Romania (ISO: RO)
│    │    │    └─── Serbia (ISO: RS)
│    │    │
│    │    ├─── Central Europe (Sub-region)
│    │    │    ├─── Czech Republic (ISO: CZ)
│    │    │    ├─── Hungary (ISO: HU)
│    │    │    ├─── Poland (ISO: PL)
│    │    │    ├─── Slovakia (ISO: SK)
│    │    │    └─── Slovenia (ISO: SI)
│    │    │
│    │    ├─── Baltic States (Sub-region)
│    │    │    ├─── Estonia (ISO: EE)
│    │    │    ├─── Latvia (ISO: LV)
│    │    │    └─── Lithuania (ISO: LT)
│    │    │
│    │    └─── Eastern Europe Proper (Sub-region)
│    │         ├─── Belarus (ISO: BY)
│    │         ├─── Moldova (ISO: MD)
│    │         ├─── Russia (European part, ISO: RU)
│    │         └─── Ukraine (ISO: UA)
│    │
│    ├─── WESTERN EUROPE (Geographic Region)
│    │    ├─── Belgium (ISO: BE)
│    │    ├─── France (ISO: FR)
│    │    ├─── Germany (ISO: DE)
│    │    ├─── Luxembourg (ISO: LU)
│    │    ├─── Netherlands (ISO: NL)
│    │    ├─── Austria (ISO: AT)
│    │    └─── Switzerland (ISO: CH)
│    │
│    ├─── NORTHERN EUROPE (Geographic Region)
│    │    ├─── Nordic Countries (Sub-region)
│    │    │    ├─── Denmark (ISO: DK)
│    │    │    ├─── Finland (ISO: FI)
│    │    │    ├─── Iceland (ISO: IS)
│    │    │    ├─── Norway (ISO: NO)
│    │    │    └─── Sweden (ISO: SE)
│    │    ├─── United Kingdom (ISO: GB)
│    │    └─── Ireland (ISO: IE)
│    │
│    └─── SOUTHERN EUROPE (Geographic Region)
│         ├─── Iberian Peninsula (Sub-region)
│         │    ├─── Portugal (ISO: PT)
│         │    ├─── Spain (ISO: ES)
│         │    └─── Andorra (ISO: AD)
│         ├─── Italian Peninsula (Sub-region)
│         │    ├─── Italy (ISO: IT)
│         │    ├─── San Marino (ISO: SM)
│         │    └─── Vatican City (ISO: VA)
│         └─── Mediterranean Europe (Sub-region)
│              ├─── Greece (ISO: GR)
│              ├─── Malta (ISO: MT)
│              └─── Cyprus (ISO: CY)
│
├─── ASIA (Continent)
│    │
│    ├─── ASEAN (Political/Economic Bloc - Association of Southeast Asian Nations)
│    │    ├─── Brunei
│    │    ├─── Cambodia
│    │    ├─── Indonesia
│    │    ├─── Laos
│    │    ├─── Malaysia
│    │    ├─── Myanmar
│    │    ├─── Philippines
│    │    ├─── Singapore
│    │    ├─── Thailand
│    │    └─── Vietnam
│    │
│    ├─── EAST ASIA (Geographic Region)
│    │    ├─── China (ISO: CN)
│    │    ├─── Japan (ISO: JP)
│    │    ├─── Mongolia (ISO: MN)
│    │    ├─── North Korea (ISO: KP)
│    │    ├─── South Korea (ISO: KR)
│    │    └─── Taiwan (ISO: TW)
│    │
│    ├─── SOUTHEAST ASIA (Geographic Region)
│    │    ├─── Brunei (ISO: BN)
│    │    ├─── Cambodia (ISO: KH)
│    │    ├─── Indonesia (ISO: ID)
│    │    ├─── Laos (ISO: LA)
│    │    ├─── Malaysia (ISO: MY)
│    │    ├─── Myanmar (ISO: MM)
│    │    ├─── Philippines (ISO: PH)
│    │    ├─── Singapore (ISO: SG)
│    │    ├─── Thailand (ISO: TH)
│    │    ├─── Timor-Leste (ISO: TL)
│    │    └─── Vietnam (ISO: VN)
│    │
│    ├─── SOUTH ASIA (Geographic Region)
│    │    ├─── Afghanistan (ISO: AF)
│    │    ├─── Bangladesh (ISO: BD)
│    │    ├─── Bhutan (ISO: BT)
│    │    ├─── India (ISO: IN)
│    │    ├─── Maldives (ISO: MV)
│    │    ├─── Nepal (ISO: NP)
│    │    ├─── Pakistan (ISO: PK)
│    │    └─── Sri Lanka (ISO: LK)
│    │
│    ├─── CENTRAL ASIA (Geographic Region)
│    │    ├─── Kazakhstan (ISO: KZ)
│    │    ├─── Kyrgyzstan (ISO: KG)
│    │    ├─── Tajikistan (ISO: TJ)
│    │    ├─── Turkmenistan (ISO: TM)
│    │    └─── Uzbekistan (ISO: UZ)
│    │
│    └─── WESTERN ASIA / MIDDLE EAST (Geographic Region)
│         ├─── Bahrain (ISO: BH)
│         ├─── Cyprus (ISO: CY)
│         ├─── Iran (ISO: IR)
│         ├─── Iraq (ISO: IQ)
│         ├─── Israel (ISO: IL)
│         ├─── Jordan (ISO: JO)
│         ├─── Kuwait (ISO: KW)
│         ├─── Lebanon (ISO: LB)
│         ├─── Oman (ISO: OM)
│         ├─── Palestine (ISO: PS)
│         ├─── Qatar (ISO: QA)
│         ├─── Saudi Arabia (ISO: SA)
│         ├─── Syria (ISO: SY)
│         ├─── Turkey (ISO: TR)
│         ├─── United Arab Emirates (ISO: AE)
│         └─── Yemen (ISO: YE)
│
├─── AFRICA (Continent)
│    │
│    ├─── AFRICAN UNION (Political Bloc - 55 member states)
│    │
│    ├─── EASTERN AFRICA (Geographic Region)
│    │    ├─── East African Community (Sub-region - Economic bloc)
│    │    │    ├─── Burundi (ISO: BI)
│    │    │    ├─── Kenya (ISO: KE)
│    │    │    │    ├─── Nairobi County
│    │    │    │    │    └─── Nairobi (city)
│    │    │    │    ├─── Mombasa County
│    │    │    │    └─── Kisumu County
│    │    │    ├─── Rwanda (ISO: RW)
│    │    │    ├─── South Sudan (ISO: SS)
│    │    │    ├─── Tanzania (ISO: TZ)
│    │    │    └─── Uganda (ISO: UG)
│    │    ├─── Horn of Africa (Sub-region)
│    │    │    ├─── Djibouti (ISO: DJ)
│    │    │    ├─── Eritrea (ISO: ER)
│    │    │    ├─── Ethiopia (ISO: ET)
│    │    │    └─── Somalia (ISO: SO)
│    │    └─── [Other Eastern African countries]
│    │
│    ├─── WESTERN AFRICA (Geographic Region)
│    │    ├─── Benin (ISO: BJ)
│    │    ├─── Burkina Faso (ISO: BF)
│    │    ├─── Ghana (ISO: GH)
│    │    ├─── Nigeria (ISO: NG)
│    │    ├─── Senegal (ISO: SN)
│    │    └─── [Other Western African countries]
│    │
│    ├─── CENTRAL AFRICA (Geographic Region)
│    │    ├─── Cameroon (ISO: CM)
│    │    ├─── Chad (ISO: TD)
│    │    ├─── Democratic Republic of Congo (ISO: CD)
│    │    └─── [Other Central African countries]
│    │
│    ├─── SOUTHERN AFRICA (Geographic Region)
│    │    ├─── Botswana (ISO: BW)
│    │    ├─── Namibia (ISO: NA)
│    │    ├─── South Africa (ISO: ZA)
│    │    ├─── Zimbabwe (ISO: ZW)
│    │    └─── [Other Southern African countries]
│    │
│    └─── NORTHERN AFRICA (Geographic Region)
│         ├─── Algeria (ISO: DZ)
│         ├─── Egypt (ISO: EG)
│         ├─── Libya (ISO: LY)
│         ├─── Morocco (ISO: MA)
│         ├─── Sudan (ISO: SD)
│         └─── Tunisia (ISO: TN)
│
└─── OCEANIA (Continent)
     ├─── Australia (ISO: AU)
     ├─── New Zealand (ISO: NZ)
     ├─── Melanesia (Sub-region)
     │    ├─── Fiji (ISO: FJ)
     │    ├─── Papua New Guinea (ISO: PG)
     │    └─── Solomon Islands (ISO: SB)
     ├─── Micronesia (Sub-region)
     │    ├─── Guam
     │    ├─── Kiribati (ISO: KI)
     │    └─── Marshall Islands (ISO: MH)
     └─── Polynesia (Sub-region)
          ├─── Samoa (ISO: WS)
          ├─── Tonga (ISO: TO)
          └─── Tuvalu (ISO: TV)
```

---

## Classification Types

### GEOGRAPHIC (Physical location-based)
- Continents: Europe, Asia, Africa, North America, South America, Oceania
- Regions: Eastern Europe, Western Europe, East Asia, South Asia, etc.
- Sub-regions: Balkans, Nordic Countries, Central America
- Countries: Bulgaria, Kenya, Japan
- Sub-national: Provinces, Counties, Cities

### POLITICAL_BLOC (Membership-based political organizations)
- European Union (EU) - 27 members
- NATO - 30 members
- African Union - 55 members
- Arab League
- Commonwealth of Nations

### ECONOMIC_BLOC (Trade and currency unions)
- Eurozone (EUR currency users - 20 countries, subset of EU)
- ASEAN (Southeast Asian trade bloc)
- NAFTA/USMCA (North American trade)
- Mercosur (South American trade)
- East African Community (EAC)

### DEVELOPMENT_STATUS (UN and World Bank classifications)
- Least Developed Countries (LDC) - 46 countries
- Developing Countries
- Developed Countries
- High Income Countries
- Low Income Countries

---

## Example: Bulgaria's Multiple Classifications

Bulgaria belongs to multiple classification hierarchies:

**GEOGRAPHIC:**
- Continent: Europe
- Macro-region: Eastern Europe
- Sub-region: Balkans
- Country: Bulgaria (ISO: BG)
- Provinces: Sofia Province, Plovdiv Province, etc.

**POLITICAL:**
- European Union (member since 2007)
- NATO (member since 2004)
- United Nations (member)

**ECONOMIC:**
- EU Single Market (member)
- NOT Eurozone (uses BGN, not EUR)

**DEVELOPMENT:**
- Developed Country
- High-Income Country (World Bank classification)

---

## Data Model

### Core Table: GeographicLocation

```java
@Table("geographic_location")
public class GeographicLocation {

    // Identity
    @Id
    private UUID locationId;
    private String name; // "Bulgaria", "Eastern Europe", "European Union"
    private String nameLocal; // "България", "Източна Европа"
    private String code; // "BG" (ISO 3166-1), "EU", "NATO"

    // Hierarchy
    private UUID parentLocationId; // Nullable for root nodes
    private Integer level; // 0=GLOBAL, 1=CONTINENT, 2=REGION, 3=COUNTRY, 4=PROVINCE, 5=CITY
    private String path; // "/GLOBAL/EUROPE/EASTERN_EUROPE/BALKANS/BULGARIA"

    // Classification
    private GeographicType type; // CONTINENT, REGION, COUNTRY, CITY, POLITICAL_BLOC, etc.
    private ClassificationType classificationType; // GEOGRAPHIC, POLITICAL, ECONOMIC, DEVELOPMENT

    // Metadata
    private Boolean isOfficial; // true for ISO countries, false for colloquial regions
    private Boolean includesChildren; // true if membership includes all child locations
    private Set<String> alternateNames; // ["Eastern Europe", "East European Region"]
    private String description;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum GeographicType {
    GLOBAL,              // Root node
    CONTINENT,           // Europe, Asia, Africa
    REGION,              // Eastern Europe, Southeast Asia
    SUB_REGION,          // Balkans, Nordic Countries
    COUNTRY,             // Bulgaria, Kenya
    ADMIN_LEVEL_1,       // Province, State, Oblast
    ADMIN_LEVEL_2,       // County, Municipality, District
    CITY,                // Sofia, Nairobi
    POLITICAL_BLOC,      // EU, NATO, African Union
    ECONOMIC_BLOC,       // Eurozone, ASEAN
    DEVELOPMENT_CLASS    // LDC, Developed Countries
}

public enum ClassificationType {
    GEOGRAPHIC,          // Physical geography
    POLITICAL,           // Political organizations/blocs
    ECONOMIC,            // Trade blocs, currency unions
    DEVELOPMENT          // UN/World Bank classifications
}
```

### Many-to-Many: Location Membership

Countries and regions can belong to multiple parent classifications:

```java
@Table("location_membership")
public class LocationMembership {

    @Id
    private UUID id;

    private UUID locationId; // Child: Bulgaria
    private UUID groupId; // Parent: EU, Eastern Europe, Balkans, NATO

    // Historical tracking
    private LocalDate memberSince; // Nullable - for political/economic blocs
    private LocalDate memberUntil; // Nullable - for countries that left (e.g., UK from EU)

    // Classification
    private Boolean isPrimary; // True for primary geographic classification
    private String membershipType; // FULL_MEMBER, ASSOCIATE_MEMBER, OBSERVER

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Example Records:**

```
Bulgaria → Europe (GEOGRAPHIC, isPrimary=true, memberSince=null)
Bulgaria → Eastern Europe (GEOGRAPHIC, isPrimary=false, memberSince=null)
Bulgaria → Balkans (GEOGRAPHIC, isPrimary=false, memberSince=null)
Bulgaria → European Union (POLITICAL, isPrimary=false, memberSince=2007-01-01)
Bulgaria → NATO (POLITICAL, isPrimary=false, memberSince=2004-03-29)
```

### Funding Source Eligibility

```java
@Table("funding_source_geography")
public class FundingSourceGeography {

    @Id
    private UUID id;

    private UUID fundingSourceId; // FK to FundingSource
    private UUID locationId; // FK to GeographicLocation

    private Boolean includeChildren; // If true, includes all child locations in hierarchy

    // Eligibility type
    private EligibilityType type; // REQUIRED, PREFERRED, EXCLUDED

    private LocalDateTime createdAt;
}

public enum EligibilityType {
    REQUIRED,   // Must be in this location
    PREFERRED,  // Preference given to this location
    EXCLUDED    // Explicitly excluded from this location
}
```

**Example: EU-only funding**
```sql
INSERT INTO funding_source_geography
  (funding_source_id, location_id, include_children, type)
VALUES
  ('erasmus-uuid', 'european-union-uuid', true, 'REQUIRED');
```
→ Includes all 27 EU member countries automatically

**Example: Eastern Europe preferred**
```sql
INSERT INTO funding_source_geography
  (funding_source_id, location_id, include_children, type)
VALUES
  ('open-society-uuid', 'eastern-europe-uuid', true, 'PREFERRED');
```
→ Includes Bulgaria, Romania, Poland, etc., but doesn't exclude others

---

## Query Examples

### Query 1: "Show all funding sources for EU members"

```sql
SELECT DISTINCT fs.*
FROM funding_source fs
JOIN funding_source_geography fsg ON fs.funding_source_id = fsg.funding_source_id
JOIN geographic_location gl ON fsg.location_id = gl.location_id
WHERE gl.name = 'European Union'
  AND gl.type = 'POLITICAL_BLOC'
  AND fsg.include_children = true
  AND fsg.type = 'REQUIRED'
```

### Query 2: "Show all funding sources for Bulgaria"

```sql
SELECT DISTINCT fs.*
FROM funding_source fs
JOIN funding_source_geography fsg ON fs.funding_source_id = fsg.funding_source_id
JOIN location_membership lm ON fsg.location_id = lm.group_id
JOIN geographic_location gl ON lm.location_id = gl.location_id
WHERE gl.name = 'Bulgaria'
  AND gl.type = 'COUNTRY'
  AND (lm.member_until IS NULL OR lm.member_until > CURRENT_DATE) -- Active membership
  AND fsg.type IN ('REQUIRED', 'PREFERRED')

UNION

-- Also match direct Bulgaria assignments
SELECT DISTINCT fs.*
FROM funding_source fs
JOIN funding_source_geography fsg ON fs.funding_source_id = fsg.funding_source_id
JOIN geographic_location gl ON fsg.location_id = gl.location_id
WHERE gl.name = 'Bulgaria'
  AND fsg.type IN ('REQUIRED', 'PREFERRED')
```

### Query 3: "Show all funding sources for Eastern Europe excluding EU members"

```sql
SELECT DISTINCT fs.*
FROM funding_source fs
JOIN funding_source_geography fsg ON fs.funding_source_id = fsg.funding_source_id
JOIN geographic_location gl ON fsg.location_id = gl.location_id
WHERE gl.name = 'Eastern Europe'
  AND fsg.include_children = true
  AND fsg.type = 'REQUIRED'
  AND NOT EXISTS (
    -- Exclude sources that require EU membership
    SELECT 1
    FROM funding_source_geography fsg2
    JOIN geographic_location gl2 ON fsg2.location_id = gl2.location_id
    WHERE fsg2.funding_source_id = fs.funding_source_id
      AND gl2.name = 'European Union'
      AND fsg2.type = 'REQUIRED'
  )
```

---

## Real-World Funding Examples

### Example 1: Erasmus+ (EU-only)
```
Funding Source: Erasmus+ Scholarship
Geographic Eligibility:
  - European Union (POLITICAL_BLOC, REQUIRED, includeChildren=true)

Result: Only 27 EU member countries eligible
Includes: Bulgaria, Romania, France, Germany
Excludes: UK (left EU), Switzerland, Norway, Serbia
```

### Example 2: America for Bulgaria Foundation
```
Funding Source: Education Infrastructure Grant
Geographic Eligibility:
  - Bulgaria (COUNTRY, REQUIRED, includeChildren=true)

Result: Only Bulgaria eligible (all provinces and cities)
Includes: Sofia, Plovdiv, Varna, all Bulgarian cities
Excludes: All other countries
```

### Example 3: Open Society Foundation
```
Funding Source: Civil Society Support
Geographic Eligibility:
  - Eastern Europe (REGION, PREFERRED, includeChildren=true)
  - Central Asia (REGION, PREFERRED, includeChildren=true)

Result: Preference for Eastern Europe and Central Asia, but not exclusive
Includes (preferred): Bulgaria, Ukraine, Kazakhstan, Uzbekistan
Also accepts: Applications from other regions (lower priority)
```

### Example 4: EEA/Norway Grants
```
Funding Source: EEA Grants
Geographic Eligibility:
  - EU Member States (POLITICAL_BLOC, REQUIRED, includeChildren=true)
  - NOT Eurozone (ECONOMIC_BLOC, EXCLUDED, includeChildren=true)

Result: EU members that don't use EUR
Includes: Bulgaria (BGN), Romania (RON), Poland (PLN)
Excludes: France, Germany, Spain (Eurozone members)
```

---

## Seeding Strategy

### Phase 1: Critical Regions for NorthStar (Immediate)
1. **GLOBAL** (root)
2. **EUROPE** (continent)
3. **EASTERN EUROPE** (geographic region)
4. **BALKANS** (sub-region)
5. **EUROPEAN UNION** (political bloc - all 27 members)
6. **BULGARIA** (country + all 28 provinces + major cities)
   - Sofia Province → Sofia (city)
   - Plovdiv Province → Plovdiv (city)
   - Varna Province → Varna (city)
   - Burgas Province
   - [24 other provinces]
7. **Balkans Countries**: Romania, North Macedonia, Serbia, Albania, Greece
8. **NORTH AMERICA** (continent) - for US-based funders
9. **United States** (country)

### Phase 2: Expand Europe (Near-term)
- Western Europe countries
- Northern Europe + Nordic countries
- Central Europe
- Baltic States
- Eurozone (economic bloc)
- NATO (political bloc)

### Phase 3: Global Expansion (Medium-term)
- Asia (continent + regions + key countries)
- Africa (continent + regions + key countries)
- South America
- ASEAN (economic bloc)
- African Union (political bloc)

### Phase 4: Full Coverage (Long-term)
- All countries with ISO 3166-1 codes
- Major sub-national divisions (provinces, states)
- Major cities (capitals + large urban areas)

---

## Handling Edge Cases

### Multi-Classification Countries

**Greece**: Both "Balkans" and "Southern Europe"
```
Greece → Europe (GEOGRAPHIC, primary)
Greece → Eastern Europe (GEOGRAPHIC) via Balkans
Greece → Southern Europe (GEOGRAPHIC)
Greece → Balkans (GEOGRAPHIC)
Greece → EU (POLITICAL)
Greece → Eurozone (ECONOMIC)
```

**Cyprus**: Multiple continents/regions
```
Cyprus → Asia (GEOGRAPHIC, Western Asia)
Cyprus → Europe (POLITICAL, EU member)
Cyprus → Mediterranean (GEOGRAPHIC)
Cyprus → EU (POLITICAL)
Cyprus → Eurozone (ECONOMIC)
```

**Turkey**: Transcontinental
```
Turkey → Europe (GEOGRAPHIC, Thrace region)
Turkey → Asia (GEOGRAPHIC, Anatolia - primary)
Turkey → Western Asia / Middle East (GEOGRAPHIC)
```

### Historical Memberships

**United Kingdom**: Left EU in 2020
```
UK → EU (memberSince=1973-01-01, memberUntil=2020-01-31)
```

Queries for "EU members" automatically exclude UK after 2020-01-31.

### Economic Bloc Subsets

**Eurozone** is a subset of **EU**:
- All Eurozone members are EU members
- Not all EU members are Eurozone members
- Bulgaria: EU member, NOT Eurozone (uses BGN)
- Romania: EU member, NOT Eurozone (uses RON)

Model as separate classifications:
```
Bulgaria → EU (POLITICAL, memberSince=2007)
Bulgaria → NOT in location_membership for Eurozone
```

---

## Integration with Domain Model

### FundingSource Entity Update

Add geographic eligibility relationship:

```java
@Table("funding_source")
public class FundingSource {
    // ... existing fields ...

    // Geographic eligibility (many-to-many via funding_source_geography)
    // Query via JOIN
}
```

### Organization Entity Update

Add headquarters location:

```java
@Table("organization")
public class Organization {
    // ... existing fields ...

    private UUID headquartersLocationId; // FK to geographic_location (city or country)
    private String headquartersAddress; // Full address string
}
```

Example:
```
America for Bulgaria Foundation
  → headquartersLocationId: United States/Vermont/Middlebury
  → operatesIn: Bulgaria (via FundingSource eligibility)
```

---

## API Examples

### REST Endpoint: Search by Geography

```
GET /api/funding-sources?geography=Bulgaria

Response:
{
  "results": [
    {
      "fundingSourceId": "...",
      "title": "America for Bulgaria Education Grant",
      "geographicEligibility": [
        {
          "location": "Bulgaria",
          "type": "COUNTRY",
          "eligibilityType": "REQUIRED"
        }
      ]
    },
    {
      "fundingSourceId": "...",
      "title": "Erasmus+ Scholarship",
      "geographicEligibility": [
        {
          "location": "European Union",
          "type": "POLITICAL_BLOC",
          "eligibilityType": "REQUIRED"
        }
      ],
      "matchReason": "Bulgaria is EU member"
    }
  ]
}
```

### REST Endpoint: List Locations by Type

```
GET /api/geographic-locations?type=POLITICAL_BLOC

Response:
{
  "locations": [
    {
      "locationId": "...",
      "name": "European Union",
      "code": "EU",
      "type": "POLITICAL_BLOC",
      "memberCount": 27,
      "members": ["Austria", "Belgium", "Bulgaria", ...]
    },
    {
      "locationId": "...",
      "name": "NATO",
      "code": "NATO",
      "type": "POLITICAL_BLOC",
      "memberCount": 30
    }
  ]
}
```

---

## Next Steps

### Implementation Tasks

1. **Create GeographicLocation entity** (Java + Spring Data JDBC)
2. **Create LocationMembership entity** (many-to-many relationships)
3. **Create FundingSourceGeography entity** (eligibility mapping)
4. **Database schema** (Flyway migrations)
5. **Seed Phase 1 data** (Bulgaria, Balkans, EU, Eastern Europe, North America)
6. **Repository layer** (queries for eligibility matching)
7. **Service layer** (geographic eligibility logic)
8. **REST API** (search by geography, list locations)

### Open Questions

1. Should we import full ISO 3166-1/2 dataset or build incrementally?
2. Do we need sub-national divisions beyond major cities?
3. Should we track currency zones separately from Eurozone?
4. How to handle disputed territories (Kosovo, Taiwan, etc.)?

---

**Document Status**: Approved - Ready for Implementation
**Next Review**: After Phase 1 seeding complete
