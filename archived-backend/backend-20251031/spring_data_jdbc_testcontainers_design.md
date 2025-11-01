# Spring Data JDBC Integration Testing with TestContainers and PostgreSQL
## Design Document for NorthStar Funding Discovery

### Overview

This document outlines the comprehensive approach for implementing Spring Data JDBC integration tests using TestContainers and PostgreSQL. This design prioritizes **PostgreSQL-specific testing** to validate domain models, repository patterns, and database interactions with production parity.

### Key Requirements Addressed

✅ **PostgreSQL Only**: No H2 or other databases - leveraging PostgreSQL-specific features
✅ **TestContainers Integration**: Automated container management for isolated testing  
✅ **Spring Data JDBC**: Testing CrudRepository, @Query, and mapping patterns
✅ **Flyway Migrations**: Schema management consistent with production
✅ **Domain Model Validation**: Entity mapping and relationship testing
✅ **Production Parity**: PostgreSQL 16 matching Mac Studio deployment

---

## Architecture Design

### 1. Test Configuration Architecture

#### Base Test Configuration Class
```java
@TestConfiguration
@Testcontainers
public class PostgreSQLTestConfiguration {
    
    @Container
    @ServiceConnection  // Spring Boot 3.1+ automatic configuration
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("northstar_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);  // Reuse container across test classes
}
```

#### Test Profile Configuration
```yaml
# application-test.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-on-validation-error: true
  
  sql:
    init:
      mode: never  # Let Flyway handle schema
  
logging:
  level:
    org.springframework.jdbc: DEBUG
    com.northstar.funding: DEBUG
```

### 2. Testing Layer Architecture

```
src/test/java/com/northstar/funding/
├── config/
│   ├── PostgreSQLTestConfiguration.java     # Base TestContainers setup
│   └── TestDataBuilders.java               # Test data creation utilities
├── domain/
│   ├── FundingSourceRepositoryTest.java    # Repository-level tests
│   ├── ApplicationRepositoryTest.java      # Repository-level tests
│   └── DomainModelIntegrationTest.java     # Cross-entity testing
├── integration/
│   ├── PersistenceLayerIntegrationTest.java # Full persistence testing
│   └── PostgreSQLFeatureTest.java          # PostgreSQL-specific features
└── utils/
    ├── TestDataFactory.java               # Test data generation
    └── PostgreSQLAssertions.java          # Custom assertion helpers
```

### 3. Repository Testing Patterns

#### Base Repository Test Class
```java
@DataJdbcTest
@Import(PostgreSQLTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
abstract class BaseRepositoryTest {
    
    @Autowired
    protected TestEntityManager testEntityManager;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    // Common test utilities and setup methods
}
```

#### Specific Repository Test Example
```java
class FundingSourceRepositoryTest extends BaseRepositoryTest {
    
    @Autowired
    private FundingSourceRepository repository;
    
    @Test
    @DisplayName("Should save and retrieve funding source with PostgreSQL JSONB")
    void shouldSaveAndRetrieveFundingSourceWithJsonb() {
        // Test PostgreSQL JSONB functionality
    }
    
    @Test
    @DisplayName("Should execute custom query with PostgreSQL array operations") 
    void shouldExecuteCustomQueryWithArrayOperations() {
        // Test @Query methods with PostgreSQL-specific SQL
    }
}
```

### 4. PostgreSQL-Specific Testing Patterns

#### JSONB Operations Testing
```java
@Test
void shouldTestJsonbContainmentOperator() {
    // Test @> operator for JSONB containment
    String sql = "SELECT * FROM funding_sources WHERE criteria @> ?::jsonb";
    // Validate PostgreSQL JSONB query execution
}
```

#### Array Operations Testing  
```java
@Test
void shouldTestArrayContainsOperations() {
    // Test PostgreSQL array operations with ANY(), @>, etc.
    String sql = "SELECT * FROM applications WHERE tags && ?";
    // Validate PostgreSQL array functionality
}
```

#### Full-Text Search Testing
```java
@Test
void shouldTestPostgreSQLFullTextSearch() {
    // Test PostgreSQL full-text search capabilities
    String sql = "SELECT * FROM funding_sources WHERE search_vector @@ plainto_tsquery(?)";
    // Validate full-text search functionality
}
```

### 5. Test Data Management Strategy

#### Test Data Builders Pattern
```java
@Component
public class TestDataFactory {
    
    public FundingSource.Builder fundingSourceBuilder() {
        return FundingSource.builder()
            .name("Test Funding Source")
            .criteria(Map.of("industry", "technology"))
            .tags(List.of("startup", "innovation"))
            .searchVector("test vector");
    }
    
    public Application.Builder applicationBuilder() {
        return Application.builder()
            .title("Test Application")
            .status(ApplicationStatus.DRAFT)
            .metadata(Map.of("priority", "high"));
    }
}
```

#### SQL Test Data Scripts
```sql
-- test-data/funding-sources.sql
INSERT INTO funding_sources (id, name, criteria, tags, search_vector) VALUES 
(1, 'Tech Innovation Fund', '{"industry": "technology", "stage": "early"}', 
 ARRAY['startup', 'innovation'], to_tsvector('innovation technology startup'));
```

### 6. Integration Testing Architecture

#### Cross-Entity Integration Tests
```java
@SpringBootTest
@Import(PostgreSQLTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PersistenceLayerIntegrationTest {
    
    @Test
    @DisplayName("Should handle complex multi-entity operations")
    void shouldHandleComplexMultiEntityOperations() {
        // Test interactions between multiple repositories
        // Validate transaction behavior
        // Test PostgreSQL constraint enforcement
    }
}
```

### 7. Performance and Resource Management

#### Container Reuse Strategy
```java
// Static container with reuse for performance
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
    .withReuse(true)        // Reuse across test runs
    .withCreateContainerCmdModifier(cmd -> 
        cmd.getHostConfig().withShmSize(256 * 1024 * 1024L));  // 256MB shared memory
```

#### Test Execution Optimization
- **Container Startup**: ~10 seconds (first time), <1 second (reused)
- **Test Execution**: 3-5 seconds per test class  
- **Memory Usage**: ~200MB per container instance
- **Parallel Execution**: Limited by PostgreSQL connection limits

### 8. Maven Integration

#### TestContainers Dependencies (Already Present)
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

#### Test Execution Configuration
```bash
# Repository tests (focused, fast feedback)
mvn test -Dtest="*RepositoryTest"

# Integration tests (comprehensive validation)
mvn test -Dtest="*IntegrationTest"

# PostgreSQL feature tests
mvn test -Dtest="*PostgreSQLTest"

# Full test suite
mvn test
```

### 9. Testing Checklist

#### Domain Model Testing
- [ ] Entity mapping and column annotations
- [ ] JSON/JSONB field serialization/deserialization  
- [ ] PostgreSQL array field handling
- [ ] Entity relationship mapping (@MappedCollection, etc.)
- [ ] Custom converter implementations

#### Repository Testing  
- [ ] Basic CRUD operations (save, findById, findAll, delete)
- [ ] Custom @Query method execution
- [ ] PostgreSQL-specific SQL operations
- [ ] Pagination and sorting functionality
- [ ] Transaction behavior validation

#### PostgreSQL Feature Testing
- [ ] JSONB operators (@>, ?, ?&, etc.)
- [ ] Array operations (ANY, @>, && operators)  
- [ ] Full-text search functionality
- [ ] PostgreSQL-specific constraints
- [ ] Index usage validation

#### Integration Testing
- [ ] Multi-repository operations
- [ ] Transaction rollback behavior
- [ ] Concurrent access patterns
- [ ] Performance under load
- [ ] Error handling and recovery

### 10. Continuous Integration Strategy

#### CI Pipeline Configuration
```yaml
# GitHub Actions example
test:
  runs-on: ubuntu-latest
  services:
    docker:
      image: docker:20.10.12
      options: --privileged
      
  steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 25
      uses: actions/setup-java@v3
      with:
        java-version: '25'
        
    - name: Run PostgreSQL Integration Tests
      run: mvn test -Dspring.profiles.active=test
      env:
        DOCKER_HOST: unix:///var/run/docker.sock
```

### 11. Development Workflow

#### TDD Cycle with PostgreSQL
```bash
# 1. Write failing test
mvn test -Dtest="FundingSourceRepositoryTest#shouldSaveWithJsonbCriteria"

# 2. Implement feature  
# Edit repository/entity code

# 3. Verify test passes
mvn test -Dtest="FundingSourceRepositoryTest#shouldSaveWithJsonbCriteria"

# 4. Run full test suite
mvn test
```

#### Local Development Setup
1. Ensure Docker is running
2. Tests will automatically start PostgreSQL containers
3. Container reuse improves subsequent test performance
4. Use IDE test runners for individual test execution

---

## Migration from Current State

### Current State Assessment
- ✅ TestContainers dependencies already configured
- ✅ PostgreSQL production setup established
- ✅ Flyway migration framework in place
- ✅ Spring Data JDBC configured
- ❌ Need to remove H2 test dependencies
- ❌ Need to create TestContainers configuration
- ❌ Need to implement test patterns

### Implementation Phase Plan

#### Phase 1: Foundation Setup
1. Create base test configuration classes
2. Setup application-test.yml profile
3. Create test data factory utilities
4. Validate TestContainers PostgreSQL startup

#### Phase 2: Repository Testing
1. Implement base repository test class
2. Create repository-specific test classes
3. Add PostgreSQL-specific feature tests
4. Validate all existing repository functionality

#### Phase 3: Integration Testing
1. Create cross-entity integration tests
2. Test complex PostgreSQL operations
3. Validate transaction behavior
4. Performance testing and optimization

#### Phase 4: Documentation and CI
1. Update spec-kit documentation
2. Configure CI pipeline
3. Create developer guidelines
4. Establish testing standards

---

## Expected Benefits

### Development Benefits
- **Production Parity**: Tests run against same PostgreSQL version as production
- **Feature Coverage**: PostgreSQL-specific capabilities fully tested
- **Confidence**: Database behavior validated before deployment
- **Documentation**: Tests serve as living documentation of database features

### Operational Benefits
- **Reduced Bugs**: Database-related issues caught in development
- **Faster Deployment**: Higher confidence in database changes
- **Maintainability**: Clear testing patterns for future development
- **Scalability**: Foundation for testing advanced PostgreSQL features

---

## Conclusion

This PostgreSQL-focused testing approach provides comprehensive validation of Spring Data JDBC functionality while leveraging PostgreSQL's advanced capabilities. The TestContainers integration ensures production parity while maintaining developer productivity through container reuse and optimized test execution.

The architecture supports both focused repository testing for rapid development feedback and comprehensive integration testing for deployment confidence, aligning with the NorthStar Funding Discovery project's requirements for robust, PostgreSQL-native database testing.