# Test Contract: Scenario 3 - Database State Verification

**Feature**: 011-create-comprehensive-docker
**Test Class**: `DatabasePersistenceIntegrationTest`
**Priority**: Critical
**Test Type**: Database integration test

---

## Test Scenario

**Given** a search request was successfully processed
**When** a test queries the database for the session
**Then** the session MUST exist with correct metadata
**And** the session status MUST indicate "RUNNING"
**And** the creation timestamp MUST be recent (within 5 seconds)

---

## Expected Database State

```sql
SELECT session_id, status, session_type, created_at, updated_at
FROM discovery_session
WHERE session_id = ?
```

**Result**:
| Field | Expected Value | Validation |
|-------|----------------|------------|
| session_id | UUID | Not null, valid UUID format |
| status | RUNNING | Enum value |
| session_type | MANUAL | Enum value |
| created_at | Recent timestamp | Within 5 seconds of test execution |
| updated_at | Same as created_at | Not null |

---

## Assertions Checklist

- [ ] Session record exists in database
- [ ] session_id matches response UUID
- [ ] status = RUNNING
- [ ] session_type = MANUAL
- [ ] created_at within 5 seconds
- [ ] updated_at not null
