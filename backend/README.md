# NorthStar Funding Discovery Backend

**Java 25 + Spring Boot 3.5.5 + Maven 3.9.9**

Backend service for the automated funding discovery workflow with human-AI collaboration.

## Constitutional Compliance

This backend service implements the NorthStar Funding Platform Constitution:

- ✅ **Java 25 + Spring Boot 3.5.5 + Maven 3.9.9** (Technology Stack)
- ✅ **Domain-Driven Design** with "Funding Sources" ubiquitous language  
- ✅ **Virtual Threads** for I/O operations (Java 25 feature)
- ✅ **PostgreSQL** on Mac Studio (192.168.1.10)
- ✅ **Human-AI Collaboration** workflows
- ✅ **Contact Intelligence** as first-class entities
- ✅ **Complexity Management** (single monolith service)

## Architecture

### Domain-Driven Design Structure

```
src/main/java/com/northstar/funding/
├── NorthStarFundingApplication.java     # Main Spring Boot application
├── discovery/                           # Funding Discovery bounded context
│   ├── domain/                         # Domain entities and services
│   ├── application/                    # Application services and orchestration
│   ├── infrastructure/                 # External integrations (DB, AI, search engines)
│   └── web/                           # REST controllers
└── common/                             # Shared utilities and configuration
```

### Test Structure

```
src/test/java/com/northstar/funding/
├── web/                               # Contract tests for REST controllers
├── integration/                       # End-to-end integration tests
└── [package]/                        # Unit tests mirroring main structure
```

## Prerequisites

### Development Environment (MacBook M2)
- **Java 25** via SDKMAN
- **Maven 3.9.9**
- **Docker** (for local testing)
- **IDE** with Java 25 support

### Infrastructure (Mac Studio - 192.168.1.10)
- **PostgreSQL 16** on port 5432
- **LM Studio** on port 1234 (for AI integration)
- **Docker** for deployment

## Quick Start

### 1. Verify Prerequisites

```bash
# Check Java version
java -version
# Should show: openjdk version "25" or similar

# Check Maven version  
mvn -version
# Should show: Apache Maven 3.9.9

# Verify Mac Studio PostgreSQL
psql -h 192.168.1.10 -p 5432 -U northstar_user -d northstar_funding_dev
```

### 2. Build Project

```bash
# Compile and run tests
mvn clean compile

# Run unit tests
mvn test

# Run integration tests (requires Mac Studio services)
mvn integration-test
```

### 3. Run Application

```bash
# Development mode (connects to Mac Studio PostgreSQL)
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Verify Running

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Should return: {"status":"UP"}
```

## Database Setup

### Development Database (Mac Studio)

The application expects PostgreSQL running on Mac Studio (192.168.1.10:5432) with:

- **Database**: `northstar_funding_dev`  
- **User**: `northstar_user`
- **Password**: `northstar_password`

### Database Migrations

Database schema is managed with Flyway migrations in `src/main/resources/db/migration/`:

```bash
# Run migrations manually
mvn flyway:migrate

# Check migration status
mvn flyway:info
```

## Configuration Profiles

### dev (Default)
- Connects to Mac Studio PostgreSQL
- Debug logging enabled
- Basic authentication (admin/admin123)

### mac-studio (Production)
- Production PostgreSQL configuration
- Environment variable based secrets
- Reduced logging

### test
- In-memory H2 database for unit tests
- TestContainers for integration tests
- Debug logging

## API Documentation

Once running, the API is available at:

- **Base URL**: `http://localhost:8080/api`
- **Health**: `http://localhost:8080/api/actuator/health`  
- **API Endpoints**: See `specs/001-automated-funding-discovery/contracts/api-spec.yaml`

## Testing Strategy

### Test-Driven Development (TDD)

Following constitutional principles, all tests are written before implementation:

1. **Contract Tests** (`src/test/java/.../web/`) - Test REST API contracts
2. **Integration Tests** (`src/test/java/.../integration/`) - End-to-end workflows  
3. **Unit Tests** - Domain logic and business rules

### Running Tests

```bash
# Unit tests only
mvn test

# Integration tests (requires Mac Studio services)
mvn integration-test

# All tests
mvn verify
```

## Development Workflow

### Maven Commands

```bash
# Clean build
mvn clean compile

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package for deployment
mvn package

# Skip tests (not recommended)
mvn package -DskipTests
```

### Virtual Threads Configuration

Java 25 Virtual Threads are enabled by default for I/O operations:

```java
// In NorthStarFundingApplication.java
System.setProperty("spring.threads.virtual.enabled", "true");
```

## Deployment

### Docker Build

```bash
# Build Docker image
docker build -t northstar-funding-backend .

# Run with Mac Studio services
docker run -p 8080:8080 --network host northstar-funding-backend
```

### Mac Studio Deployment

The application is designed to run on Mac Studio (192.168.1.10) using Docker Compose.

## Troubleshooting

### Common Issues

1. **Java 25 not found**: Install via SDKMAN: `sdk install java 25-open`
2. **Maven version mismatch**: Ensure Maven 3.9.9: `sdk install maven 3.9.9`
3. **PostgreSQL connection failed**: Verify Mac Studio services are running
4. **Virtual threads not working**: Ensure Java 25 with `--enable-preview`

### Logs

Application logs are available at:
- **Console**: Standard output with timestamp
- **File**: `logs/northstar-funding.log` (if configured)

## Next Steps

This backend service is ready for:

1. **T002**: PostgreSQL connection configuration (already included)
2. **T007-T012**: Database schema migrations via Flyway
3. **T013-T020**: Contract test implementation (TDD approach)
4. **T025-T029**: Domain entity implementation

## Constitutional Verification

- ✅ **XML Tag Accuracy**: No XML configuration used (Java-based config)
- ✅ **DDD**: Package structure follows bounded contexts  
- ✅ **Technology Stack**: Java 25 + Spring Boot 3.5.5 + Maven 3.9.9
- ✅ **Virtual Threads**: Enabled in application properties
- ✅ **Mac Studio Integration**: PostgreSQL configured for 192.168.1.10
- ✅ **Complexity Management**: Single monolith service
