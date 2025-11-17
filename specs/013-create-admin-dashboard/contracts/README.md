# API Contracts - Feature 013

This directory contains the API contract specifications and contract tests for the Admin Dashboard Review Queue.

## Files

### candidates-api.yaml
OpenAPI 3.0 specification defining the REST API contract:
- **GET /api/candidates**: List candidates with pagination, filtering, sorting
- **PUT /api/candidates/{id}/approve**: Approve a candidate
- **PUT /api/candidates/{id}/reject**: Reject a candidate and blacklist domain

### Contract Tests (TypeScript)

Contract tests verify that the API implementation matches the OpenAPI specification. These tests are written in TypeScript using the same tools as the frontend (Axios, TypeScript).

**Tests to be created**:
1. `candidates-list.test.ts` - Tests GET /api/candidates endpoint
2. `candidates-approve.test.ts` - Tests PUT /api/candidates/{id}/approve endpoint
3. `candidates-reject.test.ts` - Tests PUT /api/candidates/{id}/reject endpoint

**Test Pattern**:
```typescript
import axios from 'axios'
import { Candidate, CandidatePage } from '../../../northstar-admin-dashboard/src/types/Candidate'

const API_BASE_URL = 'http://localhost:8080/api'

describe('GET /api/candidates', () => {
  it('should return paginated candidates with default params', async () => {
    const response = await axios.get<CandidatePage>(`${API_BASE_URL}/candidates`)

    // Assert response structure matches CandidatePageDTO
    expect(response.status).toBe(200)
    expect(response.data).toHaveProperty('content')
    expect(response.data).toHaveProperty('totalElements')
    expect(response.data).toHaveProperty('totalPages')
    expect(response.data).toHaveProperty('currentPage')
    expect(response.data).toHaveProperty('pageSize')

    // Assert content is array of Candidate objects
    expect(Array.isArray(response.data.content)).toBe(true)
    if (response.data.content.length > 0) {
      const candidate = response.data.content[0]
      expect(candidate).toHaveProperty('id')
      expect(candidate).toHaveProperty('url')
      expect(candidate).toHaveProperty('title')
      expect(candidate).toHaveProperty('confidenceScore')
      expect(candidate).toHaveProperty('status')
      expect(candidate).toHaveProperty('searchEngine')
      expect(candidate).toHaveProperty('createdAt')
    }
  })

  it('should filter by status', async () => {
    const response = await axios.get<CandidatePage>(
      `${API_BASE_URL}/candidates`,
      { params: { status: ['PENDING_CRAWL', 'CRAWLED'] } }
    )

    expect(response.status).toBe(200)
    // All returned candidates should have status PENDING_CRAWL or CRAWLED
    response.data.content.forEach(candidate => {
      expect(['PENDING_CRAWL', 'CRAWLED']).toContain(candidate.status)
    })
  })

  // More tests...
})
```

## Running Contract Tests

**Prerequisites**:
1. Spring Boot API running: `mvn spring-boot:run -pl northstar-rest-api`
2. Database running with test data

**Execute tests**:
```bash
# From northstar-admin-dashboard directory
npm test contracts/
```

**Expected Behavior**:
- **Before implementation**: All tests FAIL (API endpoints don't exist)
- **After implementation**: All tests PASS (API matches contract)

## OpenAPI Validation

You can validate the OpenAPI spec using online tools:
- Swagger Editor: https://editor.swagger.io/
- OpenAPI Validator: https://apitools.dev/swagger-parser/online/

## Next Steps

1. **Implementation phase**: Create northstar-rest-api module
2. **Implement endpoints**: Match the OpenAPI spec exactly
3. **Run contract tests**: Verify implementation matches contract
4. **Document deviations**: If any changes needed, update OpenAPI spec first

## Notes

- Contract tests are part of TDD workflow: Write tests BEFORE implementation
- Tests verify request/response schemas match TypeScript interfaces
- Tests do NOT test business logic (that's in unit tests)
- Tests verify API contract compliance only
