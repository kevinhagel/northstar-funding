# Data Model: Admin Dashboard Review Queue

**Feature**: 013 - Admin Dashboard Review Queue
**Date**: 2025-11-16
**Status**: Design Phase

## Overview

This document defines the data model for Feature 013, including DTOs (Data Transfer Objects), TypeScript interfaces, and their mappings to domain entities.

## Architecture Pattern

```
Database (PostgreSQL)
  ↓ Spring Data JDBC
Domain Entity (northstar-domain)
  ↓ CandidateDTOMapper
DTO (northstar-rest-api)
  ↓ Jackson JSON Serialization
JSON (HTTP Response)
  ↓ Axios HTTP Client
TypeScript Interface (northstar-admin-dashboard)
  ↓ Vue Component
PrimeVue DataTable
```

**Key Principle**: Domain entities NEVER leave the service layer. DTOs are the API contract boundary.

## Backend DTOs (Java)

### CandidateDTO

**Purpose**: Represents a single funding source candidate for API responses

**Location**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidateDTO.java`

**Definition**:
```java
package com.northstar.funding.rest.dto;

/**
 * DTO for FundingSourceCandidate entity.
 *
 * All complex types converted to String for JSON compatibility:
 * - UUID → String (36 chars: "123e4567-e89b-12d3-a456-426614174000")
 * - BigDecimal → String (e.g., "0.85")
 * - Enum → String (e.g., "PENDING_CRAWL")
 * - LocalDateTime → String (ISO-8601: "2025-11-16T10:30:00")
 */
public record CandidateDTO(
    String id,                 // UUID of candidate
    String url,                // Funding source URL
    String title,              // Page title from search result
    String confidenceScore,    // Confidence 0.00-1.00 (BigDecimal as String)
    String status,             // CandidateStatus enum name
    String searchEngine,       // SearchEngineType enum name
    String createdAt           // Discovery timestamp (ISO-8601)
) {}
```

**Field Details**:

| Field | Source Type | DTO Type | Example Value | Notes |
|-------|-------------|----------|---------------|-------|
| id | UUID | String | "123e4567-e89b-12d3-a456-426614174000" | 36 characters with hyphens |
| url | String | String | "https://commission.europa.eu/funding-tenders" | Full URL |
| title | String | String | "Horizon Europe - EU Funding Portal" | Page title |
| confidenceScore | BigDecimal | String | "0.85" | 2 decimal places, range 0.00-1.00 |
| status | CandidateStatus | String | "PENDING_CRAWL" | Enum name() |
| searchEngine | SearchEngineType | String | "TAVILY" | Enum name() |
| createdAt | LocalDateTime | String | "2025-11-16T10:30:00" | ISO-8601 format |

**Validation Rules**:
- `id`: Must be valid UUID format
- `url`: Must be non-empty, valid URL
- `title`: Must be non-empty
- `confidenceScore`: Must parse to BigDecimal in range 0.00-1.00
- `status`: Must be valid CandidateStatus enum value
- `searchEngine`: Must be valid SearchEngineType enum value
- `createdAt`: Must be valid ISO-8601 LocalDateTime

---

### CandidatePageDTO

**Purpose**: Paginated response containing list of candidates + metadata

**Location**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidatePageDTO.java`

**Definition**:
```java
package com.northstar.funding.rest.dto;

import java.util.List;

/**
 * Paginated response for candidate list endpoint.
 *
 * Follows Spring Data Page pattern but simplified for JSON.
 */
public record CandidatePageDTO(
    List<CandidateDTO> content,     // Candidates for current page
    int totalElements,              // Total candidates matching filters
    int totalPages,                 // Total number of pages
    int currentPage,                // Current page number (0-indexed)
    int pageSize                    // Number of items per page
) {}
```

**Field Details**:

| Field | Type | Example Value | Notes |
|-------|------|---------------|-------|
| content | List<CandidateDTO> | [CandidateDTO, ...] | Array of candidates (0-pageSize items) |
| totalElements | int | 127 | Total candidates matching current filters |
| totalPages | int | 7 | Ceiling of totalElements / pageSize |
| currentPage | int | 2 | 0-indexed (page 3 in UI) |
| pageSize | int | 20 | Items per page |

**Example JSON Response**:
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "url": "https://commission.europa.eu/funding-tenders",
      "title": "Horizon Europe - EU Funding Portal",
      "confidenceScore": "0.85",
      "status": "PENDING_CRAWL",
      "searchEngine": "TAVILY",
      "createdAt": "2025-11-16T10:30:00"
    }
  ],
  "totalElements": 127,
  "totalPages": 7,
  "currentPage": 2,
  "pageSize": 20
}
```

---

### CandidateDTOMapper

**Purpose**: Converts between domain entities and DTOs

**Location**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidateDTOMapper.java`

**Definition**:
```java
package com.northstar.funding.rest.dto;

import com.northstar.funding.domain.FundingSourceCandidate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Maps between FundingSourceCandidate domain entity and CandidateDTO.
 *
 * Conversion rules:
 * - UUID → String via toString()
 * - BigDecimal → String via toString()
 * - Enum → String via name()
 * - LocalDateTime → String via ISO_LOCAL_DATE_TIME formatter
 */
@Service
public class CandidateDTOMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Convert domain entity to DTO for API response.
     */
    public CandidateDTO toDTO(FundingSourceCandidate entity) {
        return new CandidateDTO(
            entity.getId().toString(),
            entity.getUrl(),
            entity.getTitle(),
            entity.getConfidenceScore().toString(),
            entity.getStatus().name(),
            entity.getSearchEngine().name(),
            entity.getCreatedAt().format(FORMATTER)
        );
    }

    /**
     * Convert list of domain entities to DTOs.
     */
    public List<CandidateDTO> toDTOs(List<FundingSourceCandidate> entities) {
        return entities.stream()
            .map(this::toDTO)
            .toList();
    }

    // Note: No toDomain() method - this feature only reads candidates (no create/update)
}
```

**Conversion Examples**:

| Domain Field | Domain Value | DTO Value |
|--------------|--------------|-----------|
| id (UUID) | UUID.fromString("123e4567...") | "123e4567-e89b-12d3-a456-426614174000" |
| confidenceScore (BigDecimal) | new BigDecimal("0.85") | "0.85" |
| status (CandidateStatus) | CandidateStatus.PENDING_CRAWL | "PENDING_CRAWL" |
| createdAt (LocalDateTime) | LocalDateTime.of(2025, 11, 16, 10, 30) | "2025-11-16T10:30:00" |

---

## Frontend Types (TypeScript)

### Candidate Interface

**Purpose**: TypeScript representation of CandidateDTO (mirrors Java DTO exactly)

**Location**: `northstar-admin-dashboard/src/types/Candidate.ts`

**Definition**:
```typescript
/**
 * Candidate interface - mirrors CandidateDTO from backend.
 *
 * All fields are strings to match JSON serialization from Java.
 */
export interface Candidate {
  id: string                 // UUID as string
  url: string                // Funding source URL
  title: string              // Page title
  confidenceScore: string    // BigDecimal as string (e.g., "0.85")
  status: string             // CandidateStatus enum name
  searchEngine: string       // SearchEngineType enum name
  createdAt: string          // ISO-8601 LocalDateTime
}
```

**Type Guards** (optional for runtime validation):
```typescript
export function isCandidate(obj: any): obj is Candidate {
  return (
    typeof obj === 'object' &&
    typeof obj.id === 'string' &&
    typeof obj.url === 'string' &&
    typeof obj.title === 'string' &&
    typeof obj.confidenceScore === 'string' &&
    typeof obj.status === 'string' &&
    typeof obj.searchEngine === 'string' &&
    typeof obj.createdAt === 'string'
  )
}
```

---

### CandidatePage Interface

**Purpose**: TypeScript representation of CandidatePageDTO

**Location**: `northstar-admin-dashboard/src/types/CandidatePage.ts`

**Definition**:
```typescript
import { Candidate } from './Candidate'

/**
 * Paginated response for candidate list.
 * Mirrors CandidatePageDTO from backend.
 */
export interface CandidatePage {
  content: Candidate[]       // Array of candidates
  totalElements: number      // Total matching filters
  totalPages: number         // Total pages
  currentPage: number        // Current page (0-indexed)
  pageSize: number           // Items per page
}
```

---

### CandidateStatus Enum (TypeScript)

**Purpose**: TypeScript representation of Java CandidateStatus enum (for type safety in filters)

**Location**: `northstar-admin-dashboard/src/types/CandidateStatus.ts`

**Definition**:
```typescript
/**
 * Candidate status values - must match Java CandidateStatus enum.
 */
export enum CandidateStatus {
  NEW = 'NEW',
  PENDING_CRAWL = 'PENDING_CRAWL',
  CRAWLED = 'CRAWLED',
  ENHANCED = 'ENHANCED',
  JUDGED = 'JUDGED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SKIPPED_LOW_CONFIDENCE = 'SKIPPED_LOW_CONFIDENCE',
  BLACKLISTED = 'BLACKLISTED'
}

/**
 * Array of all status values for dropdown options.
 */
export const ALL_STATUSES = Object.values(CandidateStatus)
```

---

### SearchEngineType Enum (TypeScript)

**Purpose**: TypeScript representation of Java SearchEngineType enum

**Location**: `northstar-admin-dashboard/src/types/SearchEngineType.ts`

**Definition**:
```typescript
/**
 * Search engine types - must match Java SearchEngineType enum.
 */
export enum SearchEngineType {
  BRAVE = 'BRAVE',
  TAVILY = 'TAVILY',
  PERPLEXITY = 'PERPLEXITY',
  SEARXNG = 'SEARXNG',
  BROWSERBASE = 'BROWSERBASE'
}

/**
 * Array of all engine values for dropdown options.
 */
export const ALL_ENGINES = Object.values(SearchEngineType)
```

---

## Domain Entities (Existing)

These entities already exist in `northstar-domain` module. DTOs map to them.

### FundingSourceCandidate

**Source**: `northstar-domain/src/main/java/com/northstar/funding/domain/FundingSourceCandidate.java`

**Key Fields Used in Feature 013**:
```java
@Data
@Table("funding_source_candidate")
public class FundingSourceCandidate {
    @Id
    private UUID id;

    private String url;
    private String title;
    private BigDecimal confidenceScore;  // NUMERIC(3,2) - 0.00 to 1.00
    private CandidateStatus status;
    private SearchEngineType searchEngine;
    private UUID domainId;               // Foreign key to Domain
    private UUID sessionId;              // Foreign key to DiscoverySession
    private LocalDateTime createdAt;

    // Other fields not used in Feature 013...
}
```

### Domain

**Source**: `northstar-domain/src/main/java/com/northstar/funding/domain/Domain.java`

**Used for**: Blacklist operation when rejecting candidates

**Key Fields**:
```java
@Data
@Table("domain")
public class Domain {
    @Id
    private UUID id;

    private String domainName;           // e.g., "europa.eu"
    private DomainStatus status;         // DISCOVERED, BLACKLISTED, etc.
    private BigDecimal qualityScore;
    private LocalDateTime firstSeen;

    // Other fields...
}
```

---

## API Request/Response Examples

### GET /api/candidates (List with Filters)

**Request**:
```http
GET /api/candidates?page=0&size=20&status=PENDING_CRAWL&status=CRAWLED&minConfidence=0.70&sortBy=createdAt&sortDirection=DESC
```

**Query Parameters**:
- `page`: 0 (0-indexed)
- `size`: 20 (items per page)
- `status`: PENDING_CRAWL, CRAWLED (multi-select)
- `minConfidence`: 0.70 (filter ≥ 0.70)
- `sortBy`: createdAt (column to sort by)
- `sortDirection`: DESC (descending order)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "url": "https://commission.europa.eu/funding-tenders/find-funding",
      "title": "Horizon Europe - Find Funding Opportunities",
      "confidenceScore": "0.87",
      "status": "PENDING_CRAWL",
      "searchEngine": "TAVILY",
      "createdAt": "2025-11-16T10:30:00"
    },
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "url": "https://ec.europa.eu/programmes/erasmus-plus",
      "title": "Erasmus+ Programme Guide 2025",
      "confidenceScore": "0.75",
      "status": "CRAWLED",
      "searchEngine": "BRAVE",
      "createdAt": "2025-11-16T09:15:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### PUT /api/candidates/{id}/approve (Approve Candidate)

**Request**:
```http
PUT /api/candidates/a1b2c3d4-e5f6-7890-abcd-ef1234567890/approve
```

**Response** (200 OK):
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "url": "https://commission.europa.eu/funding-tenders/find-funding",
  "title": "Horizon Europe - Find Funding Opportunities",
  "confidenceScore": "0.87",
  "status": "APPROVED",
  "searchEngine": "TAVILY",
  "createdAt": "2025-11-16T10:30:00"
}
```

**Error Response** (404 NOT FOUND):
```json
{
  "timestamp": "2025-11-16T11:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Candidate not found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "path": "/api/candidates/a1b2c3d4-e5f6-7890-abcd-ef1234567890/approve"
}
```

**Error Response** (400 BAD REQUEST):
```json
{
  "timestamp": "2025-11-16T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Candidate is already approved",
  "path": "/api/candidates/a1b2c3d4-e5f6-7890-abcd-ef1234567890/approve"
}
```

---

### PUT /api/candidates/{id}/reject (Reject and Blacklist)

**Request**:
```http
PUT /api/candidates/b2c3d4e5-f6a7-8901-bcde-f12345678901/reject
```

**Response** (200 OK):
```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "url": "https://spam-site.xyz/fake-funding",
  "title": "Get Free Money Now!!!",
  "confidenceScore": "0.25",
  "status": "REJECTED",
  "searchEngine": "SEARXNG",
  "createdAt": "2025-11-16T08:00:00"
}
```

**Side Effect**: Domain "spam-site.xyz" status updated to BLACKLISTED

---

## State Transitions

### Candidate Status Transitions

```
NEW → PENDING_CRAWL → CRAWLED → ENHANCED → JUDGED → APPROVED
                           ↓
                       REJECTED (+ domain blacklisted)

SKIPPED_LOW_CONFIDENCE (terminal state - never crawled)
BLACKLISTED (domain-level rejection - prevents future discoveries)
```

**Feature 013 Actions**:
- **Approve**: CRAWLED/ENHANCED/JUDGED → APPROVED
- **Reject**: Any status → REJECTED (+ blacklist domain)

**Future Features**:
- Feature 014: View candidate details
- Feature 015: CRAWLED → ENHANCED (add contact intelligence)
- Feature 016: ENHANCED → JUDGED (AI contact extraction)

---

## Validation Summary

**Backend Validation** (Spring Boot):
- `@Valid` annotation on request bodies
- UUID format validation
- Enum value validation
- BigDecimal range validation (0.00-1.00)

**Frontend Validation** (Vue):
- TypeScript type checking at compile time
- Runtime validation in Pinia stores
- User-friendly error messages in Toast notifications

**Database Constraints**:
- `funding_source_candidate.confidence_score`: NUMERIC(3,2) CHECK (>= 0.00 AND <= 1.00)
- `funding_source_candidate.status`: ENUM (CandidateStatus values)
- `domain.status`: ENUM (DomainStatus values)

---

## Testing Strategy

**Backend Unit Tests**:
- `CandidateDTOMapperTest`: Test DTO ↔ Domain conversions
- `CandidateServiceTest`: Test business logic with mocked repositories

**Backend Integration Tests** (Optional for Feature 013):
- `CandidateControllerTest`: Test REST endpoints with TestContainers

**Frontend Unit Tests** (Optional for Feature 013):
- `candidateStore.test.ts`: Test Pinia store actions

**Contract Tests**: See `contracts/` directory

---

## Summary

**DTOs Created**:
1. `CandidateDTO` (Java record)
2. `CandidatePageDTO` (Java record)
3. `CandidateDTOMapper` (Spring service)

**TypeScript Interfaces Created**:
1. `Candidate` (mirrors CandidateDTO)
2. `CandidatePage` (mirrors CandidatePageDTO)
3. `CandidateStatus` (enum)
4. `SearchEngineType` (enum)

**Key Pattern**: All complex types → String for JSON compatibility
- UUID → String (36 chars)
- BigDecimal → String (e.g., "0.85")
- Enum → String (e.g., "PENDING_CRAWL")
- LocalDateTime → String (ISO-8601)

**Next**: Generate OpenAPI contracts and contract tests
