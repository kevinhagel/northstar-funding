# Docker Setup for Integration Tests

This document explains how to configure Docker for running integration tests in the NorthStar Funding project.

## Overview

The project uses **TestContainers** for integration tests, which require Docker to be running. TestContainers automatically starts PostgreSQL and Kafka containers during test execution.

## Prerequisites

- Java 25 (Oracle JDK via SDKMAN)
- Maven 3.9+
- Docker (either local or remote)

## Option 1: Local Docker Desktop (Recommended)

**MacBook M2 Development Machine**

### Installation

1. Download Docker Desktop for Mac (Apple Silicon): https://www.docker.com/products/docker-desktop/
2. Install and start Docker Desktop
3. Verify installation:
   ```bash
   docker --version
   docker ps
   ```

### TestContainers Configuration

TestContainers will automatically detect local Docker. No additional configuration needed!

**Verification:**
```bash
# Run container connectivity test
mvn test -Dtest=ContainerConnectivityTest -pl northstar-rest-api
```

Expected output:
```
✅ PostgreSQL container running at: jdbc:postgresql://localhost:XXXXX/testdb
✅ Kafka container (KRaft mode) running at: localhost:XXXXX
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## Option 2: Remote Docker (Mac Studio)

**Alternative setup using Docker on Mac Studio @ 192.168.1.10**

### Prerequisites on Mac Studio

Docker must be configured to listen on TCP port 2375:

```bash
# On Mac Studio, enable Docker API (Kevin will execute)
# Edit Docker Desktop settings or configure dockerd to bind to tcp://0.0.0.0:2375
```

### MacBook M2 Configuration

Create or edit `~/.testcontainers.properties`:

```properties
docker.host=tcp://192.168.1.10:2375
testcontainers.reuse.enable=true
```

**Verification:**
```bash
# Test Docker connectivity
docker -H tcp://192.168.1.10:2375 ps

# Run container connectivity test
mvn test -Dtest=ContainerConnectivityTest -pl northstar-rest-api
```

### Switching Between Local and Remote

To switch back to local Docker, comment out the docker.host setting:

```properties
#docker.host=tcp://192.168.1.10:2375
testcontainers.reuse.enable=true
```

## Container Images Used

### PostgreSQL 16 Alpine
- **Image**: `postgres:16-alpine`
- **Purpose**: Database for integration tests
- **Matches**: Production PostgreSQL 16 on Mac Studio
- **Features**: Flyway migrations run automatically

### Kafka 7.4.0 (KRaft Mode)
- **Image**: `confluentinc/cp-kafka:7.4.0`
- **Purpose**: Message queue for search workflow tests
- **Mode**: KRaft consensus (no Zookeeper required)
- **Matches**: Production Kafka 7.4.0 configuration
- **Features**: Modern Kafka without legacy Zookeeper dependency

## Running Integration Tests

### Run All Integration Tests
```bash
# Full integration test suite (REST API module)
mvn test -Dtest='*IntegrationTest' -pl northstar-rest-api

# Repository integration tests (Persistence module)
mvn test -Dtest='*IntegrationTest' -pl northstar-persistence
```

### Run Specific Integration Test
```bash
# Container connectivity
mvn test -Dtest=ContainerConnectivityTest -pl northstar-rest-api

# Search workflow
mvn test -Dtest=SearchWorkflowIntegrationTest -pl northstar-rest-api

# Kafka integration
mvn test -Dtest=KafkaIntegrationTest -pl northstar-rest-api

# Database persistence
mvn test -Dtest=DatabasePersistenceIntegrationTest -pl northstar-rest-api

# Domain repository
mvn test -Dtest=DomainRepositoryIntegrationTest -pl northstar-persistence
```

### Run Unit Tests Only (No Docker Required)
```bash
# Exclude integration tests
mvn test -Dtest='!*IntegrationTest'
```

## Performance Expectations

**Target**: <5 minutes for full integration test suite

**Typical Execution Times:**
- Container startup: ~30 seconds (one-time with reuse enabled)
- Per integration test: ~5-10 seconds
- Full REST API integration suite (3 test classes, 7 scenarios): ~30-45 seconds
- Repository integration tests (5 test classes, 52 tests): ~10-15 seconds

**Container Reuse:**
TestContainers is configured with `withReuse(true)`, meaning containers start once and are reused across test classes, significantly improving performance.

## Troubleshooting

### Docker Not Running
```
Error: Could not find a valid Docker environment
```

**Solution**: Start Docker Desktop or verify remote Docker is accessible:
```bash
# Local
docker ps

# Remote
docker -H tcp://192.168.1.10:2375 ps
```

### Container Startup Timeout
```
Error: Container startup failed after 60 seconds
```

**Solution**:
1. Check Docker has enough resources (4GB+ RAM recommended)
2. Pull images manually to verify network connectivity:
   ```bash
   docker pull postgres:16-alpine
   docker pull confluentinc/cp-kafka:7.4.0
   ```

### Port Conflicts
```
Error: Address already in use
```

**Solution**: Stop conflicting containers:
```bash
# List running containers
docker ps

# Stop specific container
docker stop <container-id>

# Or clean up all test containers
docker ps -a | grep testcontainers | awk '{print $1}' | xargs docker rm -f
```

### Kafka Container Fails (KRaft Mode)
```
Error: Kafka container failed to start
```

**Diagnosis**: Verify KRaft mode configuration:
```bash
# Check container logs
docker logs <kafka-container-id>
```

**Solution**: KRaft mode (no Zookeeper) is modern Kafka. If you see Zookeeper errors, the wrong container image is being used. Ensure `confluentinc/cp-kafka:7.4.0` is specified, not `confluentinc/cp-kafka:latest` with Zookeeper.

### Tests Pass Locally But Fail in CI
```
Tests run: 7, Failures: 0 (local)
Tests run: 7, Failures: 3 (CI)
```

**Diagnosis**: Timing issues or resource constraints in CI environment

**Solution**:
1. Increase test timeouts for CI
2. Ensure CI has Docker configured correctly
3. Consider disabling container reuse in CI for isolation: `withReuse(false)`

## Best Practices

### Local Development
1. **Keep Docker Desktop running** during development
2. **Enable container reuse** for fast test execution
3. **Run integration tests before committing** to catch issues early
4. **Clean up old containers periodically**:
   ```bash
   docker system prune -a
   ```

### Test Organization
- **Unit tests**: `*Test.java` (fast, no Docker)
- **Integration tests**: `*IntegrationTest.java` (slower, requires Docker)
- **Strategy**: Run unit tests frequently, integration tests before commit/push

### Resource Management
- **Container reuse**: Enabled by default (`withReuse(true)`)
- **Automatic cleanup**: TestContainers stops containers after JVM exit
- **Manual cleanup**: Use `docker ps -a | grep testcontainers` to find orphaned containers

## Architecture Decision Records

Related ADRs in `northstar-notes/decisions/`:
- **002-testcontainers-integration-test-pattern.md**: Why TestContainers over Docker Compose
- **002-domain-level-deduplication.md**: Domain-based deduplication strategy

## References

- **TestContainers Documentation**: https://www.testcontainers.org/
- **Kafka KRaft Mode**: https://kafka.apache.org/documentation/#kraft
- **PostgreSQL Docker Hub**: https://hub.docker.com/_/postgres
- **Confluent Kafka Docker Hub**: https://hub.docker.com/r/confluentinc/cp-kafka

## Support

If you encounter issues not covered in this document:
1. Check TestContainers logs in test output
2. Verify Docker is running: `docker ps`
3. Review container logs: `docker logs <container-id>`
4. Consult the TestContainers FAQ: https://www.testcontainers.org/supported_docker_environment/
