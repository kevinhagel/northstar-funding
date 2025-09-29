# Testing Strategy: TDD Database Layer Validation

## Current Strategy Assessment

### ✅ **Excellent Foundation - Keep H2 Tests**
Your existing repository tests are comprehensive and well-designed:
- Fast execution (2-3 seconds)
- Complete CRUD coverage
- JSON serialization testing
- Pagination and sorting validation
- Collection handling verification
- Perfect for TDD red-green-refactor cycles

### 🚀 **Add PostgreSQL TestContainers for Production Parity**
Supplement H2 tests with PostgreSQL-specific validation:
- JSONB operators and indexing
- Array operations (`ANY`, `@>`, etc.)
- Full-text search capabilities
- PostgreSQL-specific constraints
- Exact production behavior validation

## **Recommended Hybrid Approach**

### **1. Fast TDD Cycle: H2 In-Memory Tests**
```bash
# Run H2 tests for quick feedback during development
mvn test -Dtest="*RepositoryTest"
# Execution time: ~10-15 seconds for all repository tests
```

**Use H2 for:**
- Basic CRUD operations validation
- Business logic testing
- Domain model validation
- Collection and JSON field testing
- Rapid TDD feedback loops

### **2. Production Validation: PostgreSQL TestContainers**
```bash
# Run PostgreSQL tests for production parity validation
mvn test -Dtest="*RepositoryPostgresTest"
# Execution time: ~30-45 seconds (includes container startup)
```

**Use PostgreSQL TestContainers for:**
- PostgreSQL-specific SQL features
- Performance characteristics validation
- Index usage verification
- Production deployment confidence
- Complex JSON/array queries

### **3. CI/CD Integration Strategy**
```yaml
# Example GitHub Actions strategy
test:
  strategy:
    matrix:
      test-type: [unit-h2, integration-postgres]
  steps:
    - name: Unit Tests (H2)
      if: matrix.test-type == 'unit-h2'
      run: mvn test -Dtest="*Test" -Dspring.profiles.active=test
      
    - name: Integration Tests (PostgreSQL)
      if: matrix.test-type == 'integration-postgres'  
      run: mvn test -Dtest="*PostgresTest" -Dspring.profiles.active=postgres-test
```

## **Implementation Guide**

### **Step 1: Keep Your Existing H2 Tests** ✅
No changes needed. Your current tests are excellent:
```java
@DataJdbcTest
@Sql(scripts = "/test-data/funding-source-candidates.sql")
class FundingSourceCandidateRepositoryTest {
    // Your comprehensive tests are perfect for TDD
}
```

### **Step 2: Add PostgreSQL TestContainers** ✅ 
Already implemented:
```java
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FundingSourceCandidateRepositoryPostgresTest extends PostgresTestContainerConfig {
    // PostgreSQL-specific feature testing
}
```

### **Step 3: Configure Test Profiles**
Update `application.yml` with PostgreSQL test profile:

```yaml
---
# PostgreSQL TestContainers Profile
spring:
  config:
    activate:
      on-profile: postgres-test
  
  # TestContainers will override these properties dynamically
  datasource:
    url: jdbc:postgresql://localhost:5432/test  # Will be overridden
    username: test_user                         # Will be overridden  
    password: test_password                     # Will be overridden
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Let Flyway handle schema
  
  flyway:
    enabled: true  # Important: Enable migrations for TestContainers
    
# Enhanced logging for PostgreSQL testing
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.testcontainers: INFO
    com.northstar.funding: DEBUG
```

### **Step 4: Selective Test Execution**

**Development Workflow:**
```bash
# Quick TDD feedback (H2)
mvn test -Dtest="*RepositoryTest"

# Production validation (PostgreSQL)  
mvn test -Dtest="*RepositoryPostgresTest"

# Full test suite
mvn test
```

**IDE Configuration:**
- H2 tests run with `test` profile (default)
- PostgreSQL tests run with `postgres-test` profile
- Both can run simultaneously without conflicts

## **Performance Characteristics**

### **H2 Tests (Current)**
- **Startup time**: <1 second
- **Execution time**: 2-3 seconds per test class
- **Total repository tests**: ~15 seconds
- **Memory usage**: ~50MB

### **PostgreSQL TestContainers** 
- **Container startup**: ~10 seconds (first time)
- **Container reuse**: <1 second (subsequent tests)
- **Execution time**: 3-5 seconds per test class  
- **Total PostgreSQL tests**: ~30 seconds
- **Memory usage**: ~200MB

## **When to Use Each Approach**

### **Use H2 Tests For:**
✅ **Daily TDD development**
✅ **Business logic validation**  
✅ **Domain model testing**
✅ **Quick regression testing**
✅ **CI/CD unit test phase**

### **Use PostgreSQL Tests For:**
✅ **Production deployment validation**
✅ **PostgreSQL-specific features**
✅ **Performance characteristic testing**
✅ **Index usage verification**
✅ **Pre-production integration testing**

## **Quality Gates**

### **Development Phase**
1. **Red-Green-Refactor**: Use H2 tests for rapid feedback
2. **Feature completion**: Run PostgreSQL tests for validation
3. **Before commit**: Run both test suites

### **CI/CD Pipeline**
1. **Pull Request**: H2 tests (fast feedback)
2. **Merge to main**: Full test suite (H2 + PostgreSQL)
3. **Deployment pipeline**: PostgreSQL integration tests
4. **Production deployment**: Confidence from TestContainers validation

## **Maintenance Strategy**

### **Test Coverage Parity**
- **Core business logic**: Covered by both H2 and PostgreSQL tests
- **Database-specific features**: PostgreSQL tests only
- **Performance testing**: PostgreSQL tests only

### **Test Data Management**
- **H2 tests**: Use existing SQL scripts in `/test-data/`
- **PostgreSQL tests**: Use Flyway migrations + minimal test data
- **No shared test data** between H2 and PostgreSQL tests

## **Production Deployment Confidence**

### **Pre-Deployment Validation**
```bash
# Run PostgreSQL tests against exact production schema
mvn test -Dtest="*PostgresTest" -Dspring.profiles.active=postgres-test

# Verify performance characteristics
mvn test -Dtest="*RepositoryPostgresTest#shouldVerifyPostgresJsonIndexingPerformance"
```

### **Mac Studio Deployment Readiness**
- TestContainers PostgreSQL 15 matches Mac Studio PostgreSQL 15
- Schema migrations tested via Flyway in TestContainers
- Index performance validated
- JSON/JSONB operations verified

---

## **Conclusion: Best of Both Worlds**

Your project benefits from a **dual-strategy approach**:

1. **H2 for Development Speed**: Keep your excellent TDD workflow
2. **PostgreSQL for Production Confidence**: Add TestContainers validation
3. **Selective Testing**: Choose the right tool for each scenario
4. **CI/CD Flexibility**: Run appropriate tests at appropriate times

This hybrid approach maximizes both **development velocity** and **deployment confidence** while leveraging your existing investment in comprehensive repository testing.
