# Future TODO: Consider Consul Migration

**Status**: Deferred to future
**Created**: 2025-11-01
**Context**: Feature 003 implementation (Search Provider Adapters)

## Background

During Feature 003 implementation, we discussed migrating from `@ConfigurationProperties` + `application.properties` to Consul for configuration management.

## Current Approach (T001-T009)

Using Spring Boot `@ConfigurationProperties(prefix = "search.providers")` with:
- `application.properties` for defaults
- `application-prod.properties` (gitignored) for production secrets
- `application-test.properties` for test overrides

## Why We Deferred Consul

1. **Testing simplicity**: TDD phase (T010-T047) needs fast, simple tests
   - No TestContainers Consul container needed
   - No Consul client setup in tests
   - Simple `@TestPropertySource` overrides

2. **Single deployment**: Mac Studio only, not distributed system

3. **Easy future migration**: Can add `@RefreshScope` later without changing structure

## When to Reconsider

Consider Consul when:
- Multiple deployment targets (not just Mac Studio)
- Need dynamic config refresh without app restart
- Managing multiple applications with shared config
- Security requirements need Consul ACLs/encryption

## Migration Path (if needed)

```java
// Keep existing SearchProviderConfig.java as-is
@Configuration
@ConfigurationProperties(prefix = "search.providers")
@RefreshScope  // Add this annotation
public class SearchProviderConfig {
    // No code changes needed
}
```

Spring Cloud Consul can overlay Consul values on top of application.properties.

## Related

- Early SpringCrawler project used Consul extensively
- `SearchProviderConfig.java` - Current implementation
- Feature 003 tasks.md - Test requirements driving this decision

## Tags

#future-todo #configuration #consul #spring-cloud #technical-debt
