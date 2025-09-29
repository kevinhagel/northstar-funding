# Service Layer Design: Supporting Persistence for Controller Layer

**Created**: 2025-09-24  
**Status**: Critical Architecture Document  
**Context**: Service Layer Requirements for Controller Implementation

## Service Layer Architecture Requirements

### **Why Service Layer is Critical for Controllers**

**Controllers must delegate ALL business logic to services.** Controllers should be thin layers that:
- Accept HTTP requests and validate basic parameters
- Delegate business operations to appropriate services  
- Handle HTTP response formatting and error translation
- Manage request/response serialization

**Controllers CANNOT:**
- Directly access repositories (violates layered architecture)
- Contain business logic or validation rules
- Handle complex data transformations
- Manage transactions or coordinate multiple data operations

### **Service Layer Responsibilities**

#### **1. Business Rule Enforcement**
Services must validate and enforce all domain business rules:
- Candidate assignment rules (reviewers can only take limited workload)
- Status transition validation (PENDING_REVIEW → IN_REVIEW → APPROVED/REJECTED)
- Authorization checks (reviewers can only access assigned candidates)
- Data integrity validation (required fields, format validation)

#### **2. Transaction Coordination**  
Services orchestrate multiple repository operations within single transactions:
- Candidate approval updates candidate status AND decrements reviewer workload
- Duplicate merging updates candidate status AND creates enhancement record
- Contact intelligence updates modify candidate AND create audit trail

#### **3. Domain Logic Orchestration**
Services implement complex business workflows:
- **Candidate Assignment Algorithm**: Find best reviewer based on workload, specialization, and availability
- **Duplicate Detection Logic**: Compare candidates across multiple criteria and merge appropriately  
- **Confidence Score Calculation**: Aggregate multiple data quality metrics into single score
- **Audit Trail Management**: Track all changes with appropriate metadata

#### **4. Data Transformation**
Services transform between different data representations:
- Convert repository entities to service DTOs for business operations
- Aggregate data from multiple repositories into business domain objects
- Transform external API data into internal domain representations
- Calculate derived values and business metrics

#### **5. Security and Authorization**
Services enforce security policies at business layer:
- Reviewer can only access assigned candidates or unassigned candidates
- Admin users can perform system administration functions
- Contact intelligence access requires appropriate permissions
- Enhancement operations require proper authorization

### **Critical Service Implementations Needed**

#### **CandidateValidationService** (T033 - Already Implemented ✅)
**Primary Business Operations:**
- `findCandidates()` - Smart filtering with business rules
- `assignCandidateToReviewer()` - Assignment algorithm with workload balancing
- `approveCandidate()` - Status transition with audit trail
- `rejectCandidate()` - Rejection workflow with reason tracking  
- `updateCandidate()` - Data modification with enhancement tracking
- `findPotentialDuplicates()` - Duplicate detection algorithm
- `markAsDuplicate()` - Duplicate resolution workflow

**Repository Coordination:**
- FundingSourceCandidateRepository (primary operations)
- AdminUserRepository (reviewer workload management)
- EnhancementRecordRepository (audit trail creation)

#### **ContactIntelligenceService** (T034 - Missing ❌)
**Primary Business Operations:**
- `addContactToCandidate()` - Create contact with PII encryption
- `updateContactInformation()` - Modify contact with audit trail  
- `validateContactData()` - Email/phone validation and verification
- `findContactsByAuthority()` - Decision maker identification
- `trackCommunication()` - Communication history management
- `buildReferralNetwork()` - Relationship mapping and network analysis

**Repository Coordination:**
- ContactIntelligenceRepository (primary operations)
- FundingSourceCandidateRepository (candidate relationship)
- EnhancementRecordRepository (contact modification audit)

**Critical Requirements:**
- PII encryption/decryption (Constitutional requirement)
- Relationship network analysis
- Communication tracking and follow-up management

#### **DiscoveryOrchestrationService** (T035 - Missing ❌)
**Primary Business Operations:**
- `triggerDiscoverySession()` - Initiate automated discovery workflow
- `processDiscoveryResults()` - Convert raw discovery data to candidates
- `calculateConfidenceScores()` - AI data quality assessment
- `detectDuplicates()` - Cross-session duplicate identification
- `assignCandidatesForReview()` - Intelligent reviewer assignment
- `trackDiscoveryMetrics()` - Performance and quality tracking

**Repository Coordination:**
- DiscoverySessionRepository (session management)
- FundingSourceCandidateRepository (candidate creation)
- AdminUserRepository (reviewer assignment)

**Integration Requirements:**
- LM Studio AI service integration
- Multiple search engine coordination (Searxng, Tavily, Perplexity)
- Quality scoring algorithms

### **Service Layer Testing Requirements (Missing - Critical)**

#### **Business Logic Testing Approach**
```java
@ExtendWith(MockitoExtension.class)
class CandidateValidationServiceTest {
    
    @Mock
    private FundingSourceCandidateRepository candidateRepository;
    
    @Mock  
    private AdminUserRepository adminUserRepository;
    
    @InjectMocks
    private CandidateValidationService service;
    
    @Test
    void assignCandidateToReviewer_whenReviewerAvailable_assignsAndUpdatesWorkload() {
        // Test business rule: assignment updates both candidate and reviewer
    }
    
    @Test
    void assignCandidateToReviewer_whenReviewerAtCapacity_throwsBusinessException() {
        // Test business rule: workload limits enforced
    }
}
```

#### **Test Coverage Requirements**
- **Business Rule Validation**: All domain constraints tested
- **Error Handling**: Custom exceptions for business rule violations
- **Transaction Boundaries**: Multi-repository operations tested
- **Security Enforcement**: Authorization checks validated
- **Data Transformation**: Input/output transformation tested  
- **Workflow Orchestration**: Complex business processes tested

### **Controller-Service Integration Pattern**

#### **Proper Controller Implementation**
```java
@RestController
@RequestMapping("/api/candidates") 
public class CandidateController {
    
    private final CandidateValidationService candidateService;
    
    @GetMapping
    public ResponseEntity<Page<FundingSourceCandidate>> getCandidates(
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) Double minConfidence,
            Pageable pageable) {
        
        // Controller only handles HTTP concerns
        Page<FundingSourceCandidate> candidates = candidateService.findCandidates(
            status, minConfidence, null, pageable
        );
        
        return ResponseEntity.ok(candidates);
    }
}
```

#### **Service Layer Contract**
Services provide stable, testable interfaces for controllers:
- Consistent error handling with custom business exceptions
- Transactional boundaries clearly defined
- Security enforcement built into business operations
- Data validation and transformation handled internally

### **Current Status and Action Required**

#### **✅ Already Implemented:**
- CandidateValidationService with core business operations

#### **❌ Missing - Critical for Controllers:**
- **Service Layer Tests** (T032.1-T032.3) - Must implement before controller testing
- **ContactIntelligenceService** (T034) - Required for contact management endpoints
- **DiscoveryOrchestrationService** (T035) - Required for discovery workflow endpoints

#### **⚠️ Controller Development Blocked Until:**
1. Service layer tests validate business logic correctness
2. All required services are implemented and tested
3. Service-controller integration patterns are established

### **Next Actions**

**Priority 1: Service Layer Testing**
- T032.1: CandidateValidationService business logic test
- T032.2: ContactIntelligenceService business logic test
- T032.3: DiscoveryOrchestrationService workflow test

**Priority 2: Missing Service Implementation**
- T034: ContactIntelligenceService with PII encryption
- T035: DiscoveryOrchestrationService with AI integration

**Priority 3: Controller Implementation**
- Only after services are tested and working
- Controllers delegate all business logic to tested services
- Controllers focus on HTTP concerns and response formatting

This service layer design ensures controllers can reliably delegate business operations and return correct, complete results to clients.
