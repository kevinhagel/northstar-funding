# ADR 001: Use TEXT[] Arrays Instead of JSONB for Simple Collections

**Status**: Accepted
**Date**: 2025-10-21
**Context Tags**: #architecture #database #spring-data-jdbc #postgresql

## Context

When implementing the `SearchQuery` entity for Feature 003 (Search Execution Infrastructure), we needed to store two types of simple collections:

1. **Query tags** - Structured strings like "GEOGRAPHY:Bulgaria", "CATEGORY:Education", "AUTHORITY:EU"
2. **Target search engines** - Enum-like strings: "SEARXNG", "TAVILY", "PERPLEXITY"

### The Problem

Initial implementation used Java objects:
```java
private Set<QueryTag> tags;  // QueryTag is a record with type and value
```

**Spring Data JDBC interpreted this as a one-to-many relationship** and attempted to create a separate `query_tag` junction table with foreign keys. This violated our principle of "avoid complexity in early development stages."

### Requirements

- Store 3-10 simple string values per query
- Query by tag presence (e.g., find queries with "GEOGRAPHY:Bulgaria")
- Maintain type safety in Java layer
- Keep database schema simple
- Avoid ORM relationship complexity
- Support fast queries (<50ms)

### Constraints

- Using **Spring Data JDBC** (not JPA/Hibernate)
- **PostgreSQL 16** database
- Project philosophy: **"Avoid complexity in early stages of development"** (user directive)
- Java 25 with modern patterns (records, sealed types, etc.)

## Decision

**Use PostgreSQL `TEXT[]` columns to store collections, mapped to `Set<String>` in Java, with application-layer parsing for type safety.**

**Database layer:**
```sql
CREATE TABLE search_queries (
    tags TEXT[] NULL,
    target_engines TEXT[] NULL
);
```

**Java entity:**
```java
@Data
@Builder
@Table("search_queries")
public class SearchQuery {
    private Set<String> tags;  // Stored as TEXT[] in PostgreSQL
    private Set<String> targetEngines;  // Stored as TEXT[] in PostgreSQL

    // Application-layer parsing for type safety
    public List<QueryTag> getParsedTags() {
        return tags.stream()
            .map(QueryTag::fromString)  // "GEOGRAPHY:Bulgaria" -> QueryTag(GEOGRAPHY, "Bulgaria")
            .toList();
    }
}
```

**Spring Data JDBC converter:**
```java
@Component
public class QueryTagSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}
```

## Consequences

### Positive

1. **Simple database schema** - No junction tables, no foreign keys, just a single column
2. **No ORM complexity** - Spring Data JDBC doesn't try to manage relationships
3. **Fast queries** - Array queries in PostgreSQL are fast (<50ms typical)
   ```sql
   SELECT * FROM search_queries WHERE 'GEOGRAPHY:Bulgaria' = ANY(tags);
   ```
4. **Easy to understand** - Straightforward mapping: TEXT[] ↔ Set<String>
5. **Type safety in Java** - `getParsedTags()` provides strongly-typed QueryTag objects
6. **No migrations needed** - Changing tag structure doesn't require schema changes
7. **PostgreSQL native** - TEXT[] is a first-class PostgreSQL type with excellent support

### Negative

1. **Manual parsing** - Application layer must parse "TYPE:value" strings into QueryTag objects
2. **Limited flexibility** - TEXT[] not suitable for complex nested structures (fine for our use case)
3. **String validation** - Must validate tag format at application layer
4. **No database-level constraints** - Can't enforce tag format in database schema
5. **Query complexity** - Can't easily query by tag type in SQL (e.g., "all GEOGRAPHY tags")

### Neutral

1. **Future migration possible** - Can migrate to JSONB later if requirements change
2. **Not ideal for complex data** - But our data is simple (key:value pairs)
3. **Parsing overhead** - Minimal for 3-10 tags per query

## Alternatives Considered

### Alternative 1: JSONB Column
**Description**: Store tags as JSONB array with structured objects
```sql
CREATE TABLE search_queries (
    tags JSONB NULL
);
```
```json
[
  {"type": "GEOGRAPHY", "value": "Bulgaria"},
  {"type": "CATEGORY", "value": "Education"}
]
```

**Pros**:
- Flexible structure, can add fields without migration
- Rich querying with JSONB operators (`@>`, `->`, `->>`)
- Can validate structure with JSON Schema (PostgreSQL 16+)
- Better for complex nested data

**Cons**:
- **Overkill for simple key-value pairs** (our use case)
- More complex query syntax: `tags @> '[{"type":"GEOGRAPHY","value":"Bulgaria"}]'::jsonb`
- Slightly slower than array queries (marginal for our scale)
- Requires custom converters for Spring Data JDBC
- User directive: **"Avoid complexity in early stages"**

**Why we didn't choose it**: Too complex for our simple string collections. JSONB is powerful but unnecessary when TEXT[] suffices.

### Alternative 2: Separate Junction Tables
**Description**: Traditional normalized design with query_tags and search_engine_targets tables
```sql
CREATE TABLE search_queries (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE query_tags (
    query_id BIGINT REFERENCES search_queries(id),
    tag_type VARCHAR(50),
    tag_value VARCHAR(100),
    PRIMARY KEY (query_id, tag_type, tag_value)
);

CREATE TABLE query_target_engines (
    query_id BIGINT REFERENCES search_queries(id),
    engine_name VARCHAR(50),
    PRIMARY KEY (query_id, engine_name)
);
```

**Pros**:
- "Proper" normalized database design (3NF)
- Can add constraints and indexes per tag type
- Can query efficiently by tag_type
- Familiar pattern for DBAs

**Cons**:
- **3 tables instead of 1** (schema complexity)
- **Requires JOINs for every query** (performance overhead)
- **Spring Data JDBC relationship management** (tries to treat as aggregates)
- More code: repositories, entities, converters for each junction table
- Overkill for 3-10 simple string values
- User directive: **"Avoid complexity in early stages"**

**Why we didn't choose it**: Way too complex for storing a few strings. Would require ~100 lines of boilerplate code for something TEXT[] handles in 10 lines.

### Alternative 3: CSV or Delimited Strings
**Description**: Store as single VARCHAR with delimiter
```sql
CREATE TABLE search_queries (
    tags VARCHAR(500),  -- "GEOGRAPHY:Bulgaria,CATEGORY:Education"
    target_engines VARCHAR(200)  -- "SEARXNG,TAVILY,PERPLEXITY"
);
```

**Pros**:
- Simplest possible database representation
- No custom converters needed
- Easy to read in database browser

**Cons**:
- **Can't query efficiently** - No `WHERE tags LIKE '%Bulgaria%'` (matches substrings incorrectly)
- **Delimiter escaping issues** - What if tag value contains comma?
- **No PostgreSQL array support** - Can't use ANY(), ALL(), array functions
- **String manipulation** - Parsing is error-prone (split on delimiter, trim, etc.)

**Why we didn't choose it**: TEXT[] provides all the benefits of simple storage plus PostgreSQL array operators. No reason to use CSV strings when arrays exist.

## Implementation Notes

### Files Created/Modified

**Database migration:**
```sql
-- V10__create_search_queries_table.sql
CREATE TABLE search_queries (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    tags TEXT[] NULL,
    target_engines TEXT[] NULL,
    -- ...other fields
);

CREATE INDEX idx_search_queries_tags ON search_queries USING GIN(tags);
```

**Java entity:**
```java
// backend/src/main/java/com/northstar/funding/discovery/search/domain/SearchQuery.java
@Data
@Builder
@Table("search_queries")
public class SearchQuery {
    @Id
    private Long id;

    @Column("query_text")
    private String queryText;

    @Column("tags")
    private Set<String> tags;  // TEXT[] in database

    @Column("target_engines")
    private Set<String> targetEngines;  // TEXT[] in database

    // Application-layer parsing for type safety
    public List<QueryTag> getParsedTags() {
        return tags != null ? tags.stream()
            .map(QueryTag::fromString)
            .toList() : List.of();
    }

    public Set<SearchEngineType> getParsedTargetEngines() {
        return targetEngines != null ? targetEngines.stream()
            .map(SearchEngineType::valueOf)
            .collect(Collectors.toSet()) : Set.of();
    }
}
```

**Custom converters:**
```java
// backend/src/main/java/com/northstar/funding/discovery/infrastructure/converters/QueryTagSetConverter.java
@Component
public class QueryTagSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}

// backend/src/main/java/com/northstar/funding/discovery/infrastructure/converters/SearchEngineTypeSetConverter.java
@Component
public class SearchEngineTypeSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}
```

**JDBC configuration:**
```java
// backend/src/main/java/com/northstar/funding/discovery/config/JdbcConfiguration.java
@Configuration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.discovery.search.domain")
public class JdbcConfiguration extends AbstractJdbcConfiguration {
    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(
            new QueryTagSetConverter(),
            new SearchEngineTypeSetConverter()
        ));
    }
}
```

### Query Examples

**Find queries with specific geography:**
```sql
SELECT * FROM search_queries
WHERE 'GEOGRAPHY:Bulgaria' = ANY(tags);
```

**Find queries targeting Tavily:**
```sql
SELECT * FROM search_queries
WHERE 'TAVILY' = ANY(target_engines);
```

**Find queries with any geography tag:**
```sql
SELECT * FROM search_queries
WHERE EXISTS (
    SELECT 1 FROM unnest(tags) AS tag
    WHERE tag LIKE 'GEOGRAPHY:%'
);
```

### Future Migration Path

If requirements become complex (nested structures, rich querying), we can migrate to JSONB:

```sql
-- Add new JSONB column
ALTER TABLE search_queries ADD COLUMN tags_json JSONB;

-- Migrate data
UPDATE search_queries
SET tags_json = array_to_json(tags)::jsonb;

-- Add GIN index
CREATE INDEX idx_search_queries_tags_json ON search_queries USING GIN(tags_json);

-- Drop old column
ALTER TABLE search_queries DROP COLUMN tags;

-- Rename new column
ALTER TABLE search_queries RENAME COLUMN tags_json TO tags;
```

## References

- **Feature Spec**: [[../specs/003-search-execution-infrastructure/spec.md|Feature 003 Specification]]
- **Completion Summary**: [[../specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md|Feature 003 Completion]]
- **Code Files**:
  - Entity: `backend/src/main/java/com/northstar/funding/discovery/search/domain/SearchQuery.java`
  - QueryTag record: `backend/src/main/java/com/northstar/funding/discovery/search/domain/QueryTag.java`
  - Converters: `backend/src/main/java/com/northstar/funding/discovery/infrastructure/converters/QueryTagSetConverter.java`
  - Configuration: `backend/src/main/java/com/northstar/funding/discovery/config/JdbcConfiguration.java`
- **Database Migration**: `backend/src/main/resources/db/migration/V10__create_search_queries_table.sql`
- **Tests**:
  - `backend/src/test/java/com/northstar/funding/discovery/search/domain/SearchQueryTest.java`
  - `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchQueryRepositoryTest.java`
- **Spring Data JDBC Documentation**: https://docs.spring.io/spring-data/jdbc/reference/
- **PostgreSQL Arrays**: https://www.postgresql.org/docs/16/arrays.html
