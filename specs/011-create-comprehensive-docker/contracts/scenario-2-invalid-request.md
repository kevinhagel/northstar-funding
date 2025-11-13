# Test Contract: Scenario 2 - Invalid Request Handling

**Feature**: 011-create-comprehensive-docker
**Test Class**: `SearchWorkflowIntegrationTest`
**Priority**: High
**Test Type**: Error handling integration test

---

## Test Scenario

**Given** the system is running
**When** a developer submits a search request missing required fields (empty fundingSourceTypes)
**Then** the system MUST reject the request with HTTP 400 Bad Request
**And** the system MUST NOT create database records
**And** the system MUST NOT publish messages to Kafka

---

## Expected Outcomes

### HTTP Response
**Status**: 400 Bad Request

```json
{
  "error": "Validation failed",
  "field": "fundingSourceTypes",
  "message": "At least one funding source type is required"
}
```

### Database State
**Count**: 0 records in `discovery_session` table

### Kafka Events
**Count**: 0 events in `search-requests` topic

---

## Assertions Checklist

- [ ] HTTP status code = 400 Bad Request
- [ ] Response contains validation error message
- [ ] Database has 0 session records
- [ ] Kafka has 0 events
- [ ] No side effects from invalid request
