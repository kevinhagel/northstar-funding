# Quickstart: Running Integration Tests

**Feature**: 011-create-comprehensive-docker
**Date**: 2025-11-13
**Purpose**: Step-by-step guide to run Docker-based integration tests locally

---

## Prerequisites

### 1. Docker Setup

Choose **ONE** of the following options:

#### Option A: Remote Docker on Mac Studio (Recommended)
1. Verify Mac Studio Docker is running:
   ```bash
   ssh macstudio "docker ps"
   ```

2. Create `~/.testcontainers.properties` on MacBook M2:
   ```properties
   docker.host=tcp://192.168.1.10:2375
   testcontainers.reuse.enable=true
   ```

3. Test connectivity:
   ```bash
   curl -s http://192.168.1.10:2375/version
   ```

#### Option B: Local Docker Desktop
1. Install Docker Desktop for macOS
2. Start Docker Desktop
3. Verify: `docker ps`
4. No `.testcontainers.properties` needed

### 2. Java Environment
```bash
# Verify Java 25
java --version
# Should show: java 25

# If not, switch via SDKMAN
sdk use java 25
```

### 3. Maven
```bash
# Verify Maven
mvn --version
```

---

## Running Tests

### Quick Test (Unit Tests Only - No Docker)
```bash
# Fast feedback, no containers
mvn test -Dtest='!*IntegrationTest'

# Execution time: <30 seconds
```

### Integration Tests (Docker Required)
```bash
# All integration tests
mvn test -Dtest='*IntegrationTest'

# Execution time: <5 minutes (includes container startup)
```

### Specific Integration Test
```bash
# REST API workflow test
mvn test -Dtest=SearchWorkflowIntegrationTest

# Database persistence test
mvn test -Dtest=DatabasePersistenceIntegrationTest

# Repository test (fixed)
mvn test -Dtest=DomainRepositoryIntegrationTest
```

### All Tests (CI/CD Mode)
```bash
# Unit + Integration tests
mvn verify

# Execution time: <6 minutes
```

---

## What to Expect

### First Run (Container Startup)
```
[INFO] Running com.northstar.funding.rest.integration.SearchWorkflowIntegrationTest
[INFO] Testcontainers: Pulling image postgres:16-alpine
[INFO] Testcontainers: Image pull complete
[INFO] Testcontainers: Starting container postgres:16-alpine
[INFO] Testcontainers: Container started in 15.2 seconds
[INFO] Testcontainers: Pulling image confluentinc/cp-kafka:latest
[INFO] Testcontainers: Image pull complete
[INFO] Testcontainers: Starting container confluentinc/cp-kafka:latest
[INFO] Testcontainers: Container started in 12.8 seconds
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.5 s
```

### Subsequent Runs (Container Reuse)
```
[INFO] Running com.northstar.funding.rest.integration.SearchWorkflowIntegrationTest
[INFO] Testcontainers: Reusing existing container for postgres:16-alpine
[INFO] Testcontainers: Reusing existing container for confluentinc/cp-kafka:latest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.2 s
```

---

## Verifying Success

### All Tests Passing
```
[INFO] Results:
[INFO]
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Integration Test Breakdown
- **SearchWorkflowIntegrationTest**: 3 tests (successful flow, invalid request, concurrent requests)
- **DatabasePersistenceIntegrationTest**: 2 tests (session creation, no session on error)
- **KafkaIntegrationTest**: 2 tests (event publication, event structure)
- **Repository Tests**: 5 tests (Domain, FundingProgram, Organization, AdminUser, SearchResult)

---

## Troubleshooting

### "Could not find a valid Docker environment"
**Problem**: TestContainers can't connect to Docker

**Solution** (Remote Docker):
1. Verify Mac Studio Docker running: `ssh macstudio "docker ps"`
2. Test connectivity: `curl http://192.168.1.10:2375/version`
3. Check `~/.testcontainers.properties` exists with correct docker.host

**Solution** (Local Docker):
1. Start Docker Desktop
2. Verify: `docker ps`
3. Remove `~/.testcontainers.properties` if exists

### "Container failed to start"
**Problem**: Container startup timeout or error

**Check Docker Resources**:
```bash
# View running containers
docker ps

# View container logs
docker logs <container_id>

# Check Docker disk space
docker system df
```

**Solution**:
```bash
# Clean up old containers/images
docker system prune -a

# Restart Docker (if local)
# Or restart Docker service on Mac Studio
```

### "Tests are slow (>5 minutes)"
**Problem**: Container reuse not working

**Solution**:
1. Verify `testcontainers.reuse.enable=true` in ~/.testcontainers.properties
2. Check containers persisting: `docker ps -a | grep testcontainers`
3. If missing, containers are recreating each run

### Maven Build Failures
**Problem**: Compilation errors or dependency issues

**Solution**:
```bash
# Clean rebuild
mvn clean compile

# Update dependencies
mvn dependency:resolve

# Verify Java 25
java --version
```

---

## Performance Benchmarks

**Expected Timings** (MacBook M2 + Remote Docker on Mac Studio):

| Test Suite | First Run | Subsequent Runs |
|-----------|-----------|-----------------|
| Unit tests only | 20-30s | 20-30s |
| Integration tests | 3-5min | 2-3min |
| All tests (verify) | 5-6min | 3-4min |

**Container Startup** (one-time):
- PostgreSQL: ~15 seconds
- Kafka: ~12 seconds
- Total: ~30 seconds

**Test Execution** (after containers running):
- Per integration test: 5-10 seconds
- 12 integration tests: ~2 minutes

---

## Next Steps

After tests pass:
1. Review test output for any warnings
2. Check code coverage (if configured)
3. Add new integration tests following existing patterns
4. Update documentation if test scenarios change

---

## Quick Reference

```bash
# Development workflow
mvn test -Dtest='!*IntegrationTest'        # Fast unit tests
mvn test -Dtest='*IntegrationTest'         # Integration tests
mvn verify                                  # Full suite (CI/CD)

# Specific tests
mvn test -Dtest=SearchWorkflowIntegrationTest
mvn test -Dtest=DatabasePersistenceIntegrationTest

# Clean and rebuild
mvn clean package -DskipTests
mvn test -Dtest='*IntegrationTest'
```

---

## Support

**Issues with Docker setup**: See DOCKER-SETUP.md in project root

**Issues with tests**: Check test logs in `target/surefire-reports/`

**Issues with containers**: `docker ps -a` and `docker logs <container>`
