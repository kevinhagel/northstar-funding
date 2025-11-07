# ADR 003: Configuration Strategy

**Status**: Proposed
**Date**: 2025-11-07
**Context**: Session continuation - Kafka architecture planning

## Decision

Use **application.yml + Spring profiles + environment variables** for configuration management in NorthStar Funding monolithic application.

## Context

NorthStar Funding is NOT a microservices architecture and will not use Consul for configuration management. We need a simple, reliable configuration strategy that:

1. Works for local development (MacBook M2)
2. Works for deployment target (Mac Studio Docker)
3. Supports environment-specific overrides
4. Allows .env files for Docker Compose
5. Has sensible defaults that work without environment variables

Previous project (springcrawler) used Consul, which added unnecessary complexity for a monolithic application.

## Configuration Pattern

### Layer 1: Application Defaults (`application.yml`)

Base configuration with sensible defaults:

```yaml
northstar:
  # LM Studio Configuration
  lmstudio:
    base-url: ${LMSTUDIO_BASE_URL:http://192.168.1.10:1234/v1}
    model: ${LMSTUDIO_MODEL:qwen2.5-0.5b-instruct}
    api-key: ${LMSTUDIO_API_KEY:lm-studio}
    timeout-seconds: ${LMSTUDIO_TIMEOUT_SECONDS:180}
    temperature: ${LMSTUDIO_TEMPERATURE:0.7}
    max-tokens: ${LMSTUDIO_MAX_TOKENS:2048}

  # Kafka Configuration
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:192.168.1.10:9092}
    topics:
      discovered-candidates: funding-candidates-discovered
      enriched-candidates: funding-candidates-enriched
      crawled-candidates: funding-candidates-crawled
      triaged-candidates: funding-candidates-triaged
      errors: funding-errors

  # Crawler Configuration
  crawler:
    confidence-threshold: ${CRAWLER_CONFIDENCE_THRESHOLD:0.60}
    max-results-per-provider: ${CRAWLER_MAX_RESULTS:25}
    timeout-seconds: ${CRAWLER_TIMEOUT_SECONDS:30}

  # Enrichment Configuration
  enrichment:
    robots-txt-timeout-seconds: ${ENRICHMENT_ROBOTS_TIMEOUT:10}
    sitemap-timeout-seconds: ${ENRICHMENT_SITEMAP_TIMEOUT:30}
    max-sitemaps-per-domain: ${ENRICHMENT_MAX_SITEMAPS:10}
    max-urls-per-sitemap: ${ENRICHMENT_MAX_URLS:50}

  # Playwright Configuration
  playwright:
    service-url: ${PLAYWRIGHT_SERVICE_URL:http://192.168.1.10:3000}
    timeout-seconds: ${PLAYWRIGHT_TIMEOUT_SECONDS:60}
    max-concurrent-requests: ${PLAYWRIGHT_MAX_CONCURRENT:5}

  # Search Providers
  search:
    searxng:
      base-url: ${SEARXNG_BASE_URL:http://192.168.1.10:8080}
      enabled: ${SEARXNG_ENABLED:true}
    tavily:
      api-key: ${TAVILY_API_KEY:}
      enabled: ${TAVILY_ENABLED:false}
    brave:
      api-key: ${BRAVE_API_KEY:}
      enabled: ${BRAVE_ENABLED:false}
    serper:
      api-key: ${SERPER_API_KEY:}
      enabled: ${SERPER_ENABLED:false}

  # Database Configuration
  datasource:
    url: ${POSTGRES_URL:jdbc:postgresql://192.168.1.10:5432/northstar_funding}
    username: ${POSTGRES_USERNAME:northstar_user}
    password: ${POSTGRES_PASSWORD:northstar_password}

  # Virtual Threads
  virtual-threads:
    enabled: ${VIRTUAL_THREADS_ENABLED:true}
    prefix: ${VIRTUAL_THREADS_PREFIX:northstar-vt-}

spring:
  application:
    name: northstar-funding

  # Kafka Spring Boot Configuration
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:192.168.1.10:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: northstar-funding-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: com.northstar.funding.*
```

### Layer 2: Profile-Specific Configuration

**`application-dev.yml`** (Mac Studio Docker development):
```yaml
spring:
  config:
    activate:
      on-profile: dev

northstar:
  lmstudio:
    base-url: http://host.docker.internal:1234/v1
  kafka:
    bootstrap-servers: kafka:29092
  datasource:
    url: jdbc:postgresql://postgres:5432/northstar_funding
  search:
    searxng:
      base-url: http://searxng:8080
  playwright:
    service-url: http://host.docker.internal:3000

logging:
  level:
    com.northstar.funding: DEBUG
    org.springframework.kafka: DEBUG
```

**`application-prod.yml`** (Future production deployment):
```yaml
spring:
  config:
    activate:
      on-profile: prod

northstar:
  crawler:
    timeout-seconds: 60
  enrichment:
    robots-txt-timeout-seconds: 20
    sitemap-timeout-seconds: 60

logging:
  level:
    com.northstar.funding: INFO
    org.springframework.kafka: WARN
```

### Layer 3: Environment Variables (.env)

**Docker Compose `.env` file**:
```bash
# Profile Selection
SPRING_PROFILES_ACTIVE=dev

# LM Studio
LMSTUDIO_BASE_URL=http://host.docker.internal:1234/v1
LMSTUDIO_MODEL=qwen2.5-0.5b-instruct

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# PostgreSQL
POSTGRES_URL=jdbc:postgresql://postgres:5432/northstar_funding
POSTGRES_USERNAME=northstar_user
POSTGRES_PASSWORD=northstar_password

# Search Providers
SEARXNG_BASE_URL=http://searxng:8080
TAVILY_API_KEY=tvly-xxxxxxxxxxxxx
TAVILY_ENABLED=true

# Crawler Settings
CRAWLER_CONFIDENCE_THRESHOLD=0.60
CRAWLER_MAX_RESULTS=25

# Playwright
PLAYWRIGHT_SERVICE_URL=http://host.docker.internal:3000
PLAYWRIGHT_TIMEOUT_SECONDS=60
```

### Layer 4: Java System Properties (Override Everything)

For testing or emergency overrides:
```bash
java -jar northstar-application.jar \
  --spring.profiles.active=dev \
  --northstar.crawler.confidence-threshold=0.70 \
  --northstar.kafka.bootstrap-servers=localhost:9092
```

## Configuration Resolution Order

Spring Boot resolves properties in this order (highest priority first):

1. **Command line arguments** (`--property=value`)
2. **Java System Properties** (`-Dproperty=value`)
3. **OS environment variables** (`PROPERTY=value`)
4. **Profile-specific files** (`application-{profile}.yml`)
5. **Default configuration** (`application.yml`)

### Example Resolution

Property: `northstar.lmstudio.base-url`

```
application.yml:          http://192.168.1.10:1234/v1  (default)
                                    ↓ (overridden by)
application-dev.yml:      http://host.docker.internal:1234/v1
                                    ↓ (overridden by)
.env file:                http://192.168.1.11:1234/v1
                                    ↓ (overridden by)
--lmstudio.base-url=...   http://localhost:1234/v1
```

Final value: `http://localhost:1234/v1`

## Configuration Access Pattern

### Create Centralized Configuration Class

```java
package com.northstar.funding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "northstar")
@Data
@Validated
public class NorthStarProperties {

    @NotNull
    private LmStudioConfig lmstudio = new LmStudioConfig();

    @NotNull
    private KafkaConfig kafka = new KafkaConfig();

    @NotNull
    private CrawlerConfig crawler = new CrawlerConfig();

    @NotNull
    private EnrichmentConfig enrichment = new EnrichmentConfig();

    @NotNull
    private PlaywrightConfig playwright = new PlaywrightConfig();

    @NotNull
    private SearchConfig search = new SearchConfig();

    @Data
    public static class LmStudioConfig {
        @NotBlank
        private String baseUrl;

        @NotBlank
        private String model;

        @NotBlank
        private String apiKey;

        @Min(1)
        private Integer timeoutSeconds;

        private Double temperature;

        @Min(1)
        private Integer maxTokens;
    }

    @Data
    public static class KafkaConfig {
        @NotBlank
        private String bootstrapServers;

        @NotNull
        private TopicsConfig topics = new TopicsConfig();

        @Data
        public static class TopicsConfig {
            @NotBlank
            private String discoveredCandidates;

            @NotBlank
            private String enrichedCandidates;

            @NotBlank
            private String crawledCandidates;

            @NotBlank
            private String triagedCandidates;

            @NotBlank
            private String errors;
        }
    }

    @Data
    public static class CrawlerConfig {
        private Double confidenceThreshold;

        @Min(1)
        private Integer maxResultsPerProvider;

        @Min(1)
        private Integer timeoutSeconds;
    }

    @Data
    public static class EnrichmentConfig {
        @Min(1)
        private Integer robotsTxtTimeoutSeconds;

        @Min(1)
        private Integer sitemapTimeoutSeconds;

        @Min(1)
        private Integer maxSitemapsPerDomain;

        @Min(1)
        private Integer maxUrlsPerSitemap;
    }

    @Data
    public static class PlaywrightConfig {
        @NotBlank
        private String serviceUrl;

        @Min(1)
        private Integer timeoutSeconds;

        @Min(1)
        private Integer maxConcurrentRequests;
    }

    @Data
    public static class SearchConfig {
        @NotNull
        private SearxngConfig searxng = new SearxngConfig();

        @NotNull
        private TavilyConfig tavily = new TavilyConfig();

        @NotNull
        private BraveConfig brave = new BraveConfig();

        @NotNull
        private SerperConfig serper = new SerperConfig();

        @Data
        public static class SearxngConfig {
            @NotBlank
            private String baseUrl;

            private Boolean enabled;
        }

        @Data
        public static class TavilyConfig {
            private String apiKey;
            private Boolean enabled;
        }

        @Data
        public static class BraveConfig {
            private String apiKey;
            private Boolean enabled;
        }

        @Data
        public static class SerperConfig {
            private String apiKey;
            private Boolean enabled;
        }
    }
}
```

### Usage in Services

```java
@Service
@Transactional
public class SearchOrchestrationService {
    private final NorthStarProperties properties;

    public SearchOrchestrationService(NorthStarProperties properties) {
        this.properties = properties;
    }

    public void executeSearch() {
        // Access configuration
        String searxngUrl = properties.getSearch().getSearxng().getBaseUrl();
        int maxResults = properties.getCrawler().getMaxResultsPerProvider();
        double threshold = properties.getCrawler().getConfidenceThreshold();

        // Use configuration...
    }
}
```

## Benefits

1. **No External Dependencies**: No Consul, no Config Server
2. **Docker-Friendly**: .env files work naturally with docker-compose
3. **Fail-Safe Defaults**: Works without any environment variables
4. **Type-Safe**: Java configuration classes with validation
5. **IDE Support**: Autocomplete for configuration properties
6. **Testable**: Easy to override in tests
7. **Clear Precedence**: Well-understood Spring Boot property resolution

## Drawbacks

1. **No Runtime Refresh**: Need to restart application to change config (acceptable for monolith)
2. **No Centralized Management**: Each deployment has its own configuration files
3. **Manual Sync**: If deploying to multiple hosts, must sync config files manually

## Alternatives Considered

1. **Spring Cloud Config Server**: Rejected - too much infrastructure for monolith
2. **Consul**: Rejected - unnecessary complexity, used in springcrawler
3. **Environment Variables Only**: Rejected - hard to maintain, no defaults
4. **application.properties**: Rejected - YAML is more readable for nested properties

## Implementation Plan

1. Create `northstar-application/src/main/resources/application.yml` with defaults
2. Create `northstar-application/src/main/resources/application-dev.yml` for Docker profile
3. Create `NorthStarProperties.java` configuration class
4. Create `.env.example` file for Docker Compose template
5. Update `docker-compose.yml` to use `env_file: .env`
6. Document configuration in CLAUDE.md

## Migration from Existing Code

Current code uses scattered `@Value` annotations:
```java
@Value("${searxng.base-url}")
private String searxngBaseUrl;
```

Replace with:
```java
private final NorthStarProperties properties;

public MyService(NorthStarProperties properties) {
    this.properties = properties;
}

// Use: properties.getSearch().getSearxng().getBaseUrl()
```

## References

- Spring Boot Externalized Configuration: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config
- ConfigurationProperties: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties
