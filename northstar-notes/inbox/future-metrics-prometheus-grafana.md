# Future TODO: Metrics with Prometheus & Grafana

**Status**: Deferred to future
**Created**: 2025-11-02
**Context**: Feature 003 implementation (Search Provider Adapters)

## Background

During Feature 003 implementation, we discussed tracking spam analysis results. For v1, we're going with **Option A: Log and skip** (simpler, less storage). However, we want comprehensive metrics later.

## Current Approach (v1)

- Spam detected → log warning, increment session stats, skip result
- No persistent spam analysis table
- Session stats in DiscoverySession: `spamResultsFiltered` counter only

## Future Metrics Requirements

When implementing Prometheus + Grafana integration:

### 1. Spring Boot Actuator + Micrometer
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. Metrics to Track

**Search Provider Metrics**:
- `search_requests_total{provider="brave"}` - Total API calls per provider
- `search_request_duration_seconds{provider="brave"}` - Response time distribution
- `search_failures_total{provider="brave", error_type="timeout"}` - Failure counts
- `rate_limit_exceeded_total{provider="brave"}` - Rate limit hits

**Anti-Spam Metrics**:
- `spam_detected_total{indicator="keyword_stuffing"}` - Spam detections by type
- `spam_confidence_score` - Distribution of spam confidence scores
- `spam_filter_duration_seconds` - Fuzzy matching performance

**Domain Metrics**:
- `domains_discovered_total` - New domains
- `domains_blacklisted_total{reason="spam"}` - Blacklist additions
- `domain_duplicates_total` - Deduplication hits

**Session Metrics**:
- `discovery_sessions_total{type="nightly", status="completed"}` - Session counts
- `discovery_session_duration_seconds` - Session completion time
- `results_per_session` - Result count distribution

### 3. Grafana Dashboards

**Search Provider Health Dashboard**:
- Response time percentiles (p50, p95, p99) per provider
- Success/failure rates
- Rate limit utilization

**Spam Detection Dashboard**:
- Spam detection rate over time
- Breakdown by spam indicator type
- False positive tracking (manual overrides)

**Discovery Session Dashboard**:
- Sessions per day/week
- Results discovered per session
- New domains vs duplicates
- Spam filtering effectiveness

### 4. Configuration

```properties
# Prometheus endpoint
management.endpoints.web.exposure.include=health,info,prometheus
management.metrics.export.prometheus.enabled=true

# Micrometer tags (add provider, session_type to all metrics)
management.metrics.tags.application=northstar-funding
management.metrics.tags.environment=production
```

### 5. Mac Studio Setup

- Prometheus server running in Docker
- Grafana running in Docker
- Both accessible at http://192.168.1.10:9090 and :3000
- Prometheus scrapes Spring Boot actuator endpoint every 15s

## Why Deferred

1. **v1 Focus**: Get search providers working first
2. **Complexity**: Prometheus + Grafana adds deployment overhead
3. **Current Logging**: SLF4J logs provide sufficient visibility for v1
4. **Session Stats**: DiscoverySession table captures basic metrics

## When to Implement

Consider adding metrics when:
- Multiple discovery sessions running (need real-time monitoring)
- Tuning spam detection thresholds (need confidence score distributions)
- Diagnosing provider performance issues
- Monthly reporting for stakeholders

## Related

- Current logging: SLF4J in AbstractSearchProviderAdapter
- Session stats: DiscoverySession entity
- Anti-spam: AntiSpamFilter with SpamAnalysisResult
- Feature 003 spec: FR-040 mentions spam monitoring

## Tags

#future-todo #metrics #prometheus #grafana #observability #monitoring
