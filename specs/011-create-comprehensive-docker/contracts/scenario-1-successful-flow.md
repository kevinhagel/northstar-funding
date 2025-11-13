# Test Contract: Scenario 1 - Successful Search Request Flow

**Feature**: 011-create-comprehensive-docker
**Test Class**: `SearchWorkflowIntegrationTest`
**Priority**: Critical
**Test Type**: End-to-end integration test

---

## Test Scenario

**Given** the system is running with all required containers (PostgreSQL, Kafka)
**And** QueryGenerationService is mocked to return 3 queries per engine
**And** DiscoverySessionService is mocked to return a session with auto-generated UUID

**When** a developer submits a valid search request via POST /api/search/execute

**Then** the system MUST create a tracking session in the database
**And** the system MUST generate search queries (mocked, 3 per engine)
**And** the system MUST publish 9 messages to Kafka (3 engines × 3 queries)
**And** the system MUST return HTTP 200 OK with session identifier

---

## Test Implementation

### Test Method
```java
@Test
void executeSearch_WithValidRequest_CompletesFullWorkflow() {
    // GIVEN - Setup
    SearchExecutionRequest request = TestFixtures.validSearchRequest();
    setupQueryGenerationMock(); // Returns 3 queries per engine
    setupSessionServiceMock();   // Returns session with UUID

    // WHEN - Execute
    ResponseEntity<SearchExecutionResponse> response = restTemplate.postForEntity(
        "/api/search/execute",
        request,
        SearchExecutionResponse.class
    );

    // THEN - Verify response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sessionId()).isNotNull();
    assertThat(response.getBody().queriesGenerated()).isEqualTo(9); // 3 engines × 3 queries
    assertThat(response.getBody().status()).isEqualTo("INITIATED");

    // THEN - Verify database state
    UUID sessionId = response.getBody().sessionId();
    verifySessionCreatedInDatabase(sessionId);

    // THEN - Verify Kafka events
    List<SearchRequestEvent> events = consumeKafkaEvents(sessionId);
    assertThat(events).hasSize(9);
    verifyAllEventsHaveSessionId(events, sessionId);
}
```

### Setup Methods
```java
private void setupQueryGenerationMock() {
    when(queryGenerationService.generateQueries(any()))
        .thenReturn(CompletableFuture.completedFuture(
            QueryGenerationResponse.builder()
                .queries(List.of("query1", "query2", "query3"))
                .build()
        ));
}

private void setupSessionServiceMock() {
    DiscoverySession mockSession = DiscoverySession.builder()
        .sessionId(UUID.randomUUID())
        .status(SessionStatus.RUNNING)
        .sessionType(SessionType.MANUAL)
        .build();

    when(discoverySessionService.createSession(any()))
        .thenReturn(mockSession);
}
```

### Verification Methods
```java
private void verifySessionCreatedInDatabase(UUID sessionId) {
    String sql = "SELECT status, session_type, created_at FROM discovery_session WHERE session_id = ?";
    Map<String, Object> session = jdbcTemplate.queryForMap(sql, sessionId);

    assertThat(session.get("status")).isEqualTo("RUNNING");
    assertThat(session.get("session_type")).isEqualTo("MANUAL");
    assertThat(session.get("created_at")).isNotNull();
}

private List<SearchRequestEvent> consumeKafkaEvents(UUID sessionId) {
    Consumer<String, SearchRequestEvent> consumer = createKafkaConsumer();
    consumer.subscribe(Collections.singleton("search-requests"));

    ConsumerRecords<String, SearchRequestEvent> records =
        consumer.poll(Duration.ofSeconds(5));

    return StreamSupport.stream(records.spliterator(), false)
        .map(ConsumerRecord::value)
        .filter(event -> event.getSessionId().equals(sessionId))
        .collect(Collectors.toList());
}

private void verifyAllEventsHaveSessionId(List<SearchRequestEvent> events, UUID sessionId) {
    events.forEach(event -> {
        assertThat(event.getSessionId()).isEqualTo(sessionId);
        assertThat(event.getQuery()).isNotBlank();
        assertThat(event.getMaxResults()).isGreaterThanOrEqualTo(10);
    });
}
```

---

## Expected Outcomes

### HTTP Response
```json
{
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "queriesGenerated": 9,
  "status": "INITIATED",
  "message": "Search workflow initiated successfully"
}
```

### Database State
**Table**: `discovery_session`

| session_id | status | session_type | created_at | updated_at |
|------------|--------|--------------|------------|------------|
| <UUID> | RUNNING | MANUAL | <TIMESTAMP> | <TIMESTAMP> |

### Kafka Events (search-requests topic)
**Count**: 9 events (3 engines × 3 queries)

**Event Structure**:
```json
{
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "query": "Bulgaria educational funding grants",
  "maxResults": 25,
  "timestamp": "2025-11-13T10:00:00Z"
}
```

---

## Test Data

### Input Request
```json
{
  "fundingSourceTypes": ["GOVERNMENT_EU", "GOVERNMENT_NATIONAL"],
  "fundingMechanisms": ["GRANT", "SCHOLARSHIP"],
  "projectScale": "SMALL",
  "beneficiaryPopulations": ["EDUCATORS_TEACHERS"],
  "recipientOrganizationTypes": ["UNIVERSITY_PUBLIC"],
  "geographicScope": ["Bulgaria", "Eastern Europe"],
  "queryLanguage": "ENGLISH",
  "maxResultsPerQuery": 25
}
```

### Mock Responses

**QueryGenerationService** (per engine):
```java
QueryGenerationResponse.builder()
    .queries(List.of(
        "Bulgaria educational funding grants",
        "Eastern Europe teacher scholarships",
        "EU small project grants"
    ))
    .build()
```

**DiscoverySessionService**:
```java
DiscoverySession.builder()
    .sessionId(UUID.randomUUID())
    .status(SessionStatus.RUNNING)
    .sessionType(SessionType.MANUAL)
    .createdAt(Instant.now())
    .updatedAt(Instant.now())
    .build()
```

---

## Assertions Checklist

- [ ] HTTP status code = 200 OK
- [ ] Response body not null
- [ ] Response sessionId not null (UUID format)
- [ ] Response queriesGenerated = 9
- [ ] Response status = "INITIATED"
- [ ] Database has 1 session record
- [ ] Session status = RUNNING
- [ ] Session type = MANUAL
- [ ] Session created_at within 5 seconds
- [ ] Kafka has 9 events
- [ ] All events have correct sessionId
- [ ] All events have non-blank query
- [ ] All events have maxResults >= 10

---

## Performance Expectations

- Test execution time: <10 seconds
- Container startup (one-time): <30 seconds
- HTTP request processing: <1 second
- Kafka event publication: <1 second

---

## Notes

- QueryGenerationService is mocked to avoid Ollama dependency
- DiscoverySessionService is mocked to control session UUID
- Real PostgreSQL and Kafka via TestContainers
- Tests run in @Transactional context (automatic rollback)
