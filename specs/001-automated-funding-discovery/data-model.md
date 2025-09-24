# Data Model: Automated Funding Discovery Workflow

**Feature**: 001-automated-funding-discovery  
**Date**: 2025-09-22  
**Domain**: Funding Discovery Bounded Context

## Domain Entity Overview

Following Domain-Driven Design principles with "Funding Sources" ubiquitous language and Contact Intelligence as first-class entities.

```
FundingSourceCandidate (Aggregate Root)
├── ContactIntelligence (Entity)
├── DiscoverySession (Entity) 
├── EnhancementRecord (Value Object)
└── AdminUser (Entity)
```

## Core Domain Entities

### FundingSourceCandidate (Aggregate Root)
Primary entity representing discovered funding opportunities pending human validation.

**Fields**:
- `candidateId`: UUID - Primary key
- `status`: CandidateStatus - PENDING_REVIEW, IN_REVIEW, APPROVED, REJECTED
- `confidenceScore`: Double (0.0-1.0) - AI-generated confidence rating
- `discoverySessionId`: UUID - Reference to discovery audit trail
- `discoveredAt`: LocalDateTime - When initially found
- `lastUpdatedAt`: LocalDateTime - Last modification timestamp
- `assignedReviewerId`: UUID - AdminUser assigned for review (nullable)
- `reviewStartedAt`: LocalDateTime - When review began (nullable)

**Funding Source Data**:
- `organizationName`: String - Foundation, government agency, NGO, etc.
- `programName`: String - Specific funding program title
- `sourceUrl`: String - Original URL where discovered
- `description`: String - Program description/summary
- `fundingAmountMin`: BigDecimal - Minimum funding amount (nullable)
- `fundingAmountMax`: BigDecimal - Maximum funding amount (nullable)
- `currency`: String - Currency code (EUR, USD, etc.)
- `geographicEligibility`: List<String> - Eligible regions/countries
- `organizationTypes`: List<String> - Eligible applicant types
- `applicationDeadline`: LocalDate - Deadline if known (nullable)
- `applicationProcess`: String - How to apply description
- `requirements`: List<String> - Eligibility requirements
- `tags`: Set<String> - Categorization tags

**Discovery Metadata**:
- `discoveryMethod`: String - Which search engine/method found it
- `searchQuery`: String - Query that discovered this source  
- `extractedData`: JSON - Raw scraped data as JSON
- `duplicateOfCandidateId`: UUID - If duplicate, reference to master (nullable)

**Enhancement Tracking**:
- `enhancementRecords`: List<EnhancementRecord> - Audit trail of manual improvements
- `validationNotes`: String - Admin notes during review
- `rejectionReason`: String - Why rejected if applicable

### ContactIntelligence (Entity)
First-class entity for contact information associated with funding sources.

**Fields**:
- `contactId`: UUID - Primary key
- `candidateId`: UUID - Foreign key to FundingSourceCandidate
- `contactType`: ContactType - PROGRAM_OFFICER, FOUNDATION_STAFF, GOVERNMENT_OFFICIAL
- `fullName`: String - Contact person's name
- `title`: String - Job title/role
- `email`: String - Email address (encrypted at rest)
- `phone`: String - Phone number (encrypted at rest)  
- `organization`: String - Organization/department
- `officeAddress`: String - Physical office location
- `communicationPreference`: String - Email, phone, formal application only
- `lastContactedAt`: LocalDateTime - When last contacted (nullable)
- `responsePattern`: String - How they typically respond
- `referralSource`: String - How we learned about this contact
- `validatedAt`: LocalDateTime - When contact info last verified
- `isActive`: Boolean - Whether contact is still valid

**Relationship Context**:
- `decisionAuthority`: AuthorityLevel - DECISION_MAKER, INFLUENCER, INFORMATION_ONLY
- `relationshipNotes`: String - History of interactions, preferences
- `referralConnections`: String - Mutual connections or referral chains

### AdminUser (Entity)  
System administrators who review and validate funding source candidates.

**Fields**:
- `userId`: UUID - Primary key
- `username`: String - Login identifier
- `fullName`: String - Display name
- `email`: String - Contact email
- `role`: AdminRole - ADMINISTRATOR, REVIEWER
- `isActive`: Boolean - Account status
- `createdAt`: LocalDateTime - Account creation
- `lastLoginAt`: LocalDateTime - Last system access

**Review Statistics**:
- `candidatesReviewed`: Integer - Total candidates processed
- `averageReviewTime`: Duration - Average time per candidate
- `approvalRate`: Double - Percentage of approved vs rejected
- `specializations`: Set<String> - Areas of expertise for assignment

### DiscoverySession (Entity)
Audit record of automated discovery executions for tracking and improvement.

**Fields**:
- `sessionId`: UUID - Primary key  
- `executedAt`: LocalDateTime - When discovery ran
- `executedBy`: String - System/scheduler identifier
- `sessionType`: SessionType - SCHEDULED, MANUAL, RETRY
- `durationMinutes`: Integer - How long discovery took
- `status`: SessionStatus - RUNNING, COMPLETED, FAILED, CANCELLED

**Search Configuration**:
- `searchEnginesUsed`: List<String> - Which engines were queried
- `searchQueries`: List<String> - Generated queries executed
- `queryGenerationPrompt`: String - AI prompt used for query generation

**Results Summary**:
- `candidatesFound`: Integer - New candidates discovered
- `duplicatesDetected`: Integer - Duplicates filtered out
- `averageConfidenceScore`: Double - Quality of discovered candidates
- `errorMessages`: List<String> - Any errors encountered
- `sourcesScraped`: Integer - How many URLs were processed

### EnhancementRecord (Value Object)
Immutable record of manual improvements made by admin users for quality tracking.

**Fields**:
- `enhancementId`: UUID - Identifier
- `candidateId`: UUID - Which candidate was enhanced
- `enhancedBy`: UUID - AdminUser who made changes
- `enhancedAt`: LocalDateTime - When changes made
- `enhancementType`: EnhancementType - CONTACT_ADDED, DATA_CORRECTED, NOTES_ADDED
- `fieldName`: String - Which field was modified
- `oldValue`: String - Previous value (nullable)
- `newValue`: String - New value
- `notes`: String - Explanation of changes
- `timeSpentMinutes`: Integer - How long enhancement took

## Enumerations

### CandidateStatus
```
PENDING_REVIEW - Newly discovered, awaiting assignment
IN_REVIEW - Currently being reviewed by admin user
APPROVED - Validated and moved to knowledge base  
REJECTED - Not suitable for inclusion
```

### ContactType
```  
PROGRAM_OFFICER - Manages specific funding programs
FOUNDATION_STAFF - General foundation personnel
GOVERNMENT_OFFICIAL - Government agency contacts
ACADEMIC_CONTACT - University/research contacts
CORPORATE_CONTACT - Business/corporate foundation contacts
```

### AuthorityLevel
```
DECISION_MAKER - Can approve/reject applications
INFLUENCER - Influences decisions but doesn't decide
INFORMATION_ONLY - Provides info but no decision authority
```

### SessionType
```
SCHEDULED - Regular automated discovery
MANUAL - Triggered by admin user
RETRY - Retry of failed discovery session
```

### EnhancementType
```
CONTACT_ADDED - New contact information added
DATA_CORRECTED - Fixed incorrect information  
NOTES_ADDED - Added validation or context notes
DUPLICATE_MERGED - Merged duplicate candidate data
```

## Entity Relationships

### Primary Aggregates
- **FundingSourceCandidate** ← One-to-Many → **ContactIntelligence**
- **FundingSourceCandidate** ← Many-to-One → **DiscoverySession**  
- **FundingSourceCandidate** ← One-to-Many → **EnhancementRecord**
- **FundingSourceCandidate** ← Many-to-One → **AdminUser** (reviewer assignment)

### Cross-Aggregate References
- **EnhancementRecord** references **AdminUser** (who made enhancement)
- **ContactIntelligence** may reference other **ContactIntelligence** (referral chains)
- **FundingSourceCandidate** may reference other **FundingSourceCandidate** (duplicate detection)

## Repository Patterns

Following DDD repository pattern with aggregate-focused data access:

### FundingSourceCandidateRepository
- `findPendingCandidatesByConfidenceScore()` - Review queue ordered by quality
- `findDuplicatesByOrganizationAndProgram()` - Duplicate detection queries
- `findCandidatesAssignedToReviewer(UUID reviewerId)` - Admin workload queries
- `findCandidatesOlderThan(LocalDateTime threshold)` - Stale candidate cleanup

### ContactIntelligenceRepository  
- `findContactsByCandidateId(UUID candidateId)` - All contacts for funding source
- `findContactsNeedingValidation()` - Contacts older than 90 days
- `findContactsByOrganization(String organization)` - Organization-based queries

### DiscoverySessionRepository
- `findRecentSessions(int limit)` - Audit and monitoring queries
- `findFailedSessions()` - Error analysis and retry logic
- `getAverageDiscoveryMetrics()` - Performance tracking

## Domain Services

### CandidateDeduplicationService
Business logic for detecting and merging duplicate funding source candidates across discovery sessions.

### ConfidenceScoreService  
Algorithm for calculating confidence scores based on data completeness, source reliability, and extraction quality.

### ContactValidationService
Orchestrates contact intelligence validation workflows including email verification and relationship mapping.

## Data Constraints & Validation

### Business Rules
- Candidates must have organizationName and programName (required)
- ConfidenceScore must be between 0.0 and 1.0
- Contact email/phone encrypted at rest (constitutional requirement)
- Enhancement audit trail immutable once created
- Admin users can only review candidates assigned to them or unassigned

### Database Constraints
- Unique constraint on (organizationName, programName) within APPROVED status
- Foreign key constraints with cascade rules
- Index on (status, confidenceScore DESC) for review queue performance
- Index on (discoveredAt DESC) for recent candidate queries
- Index on contactIntelligence.candidateId for relationship queries

---
**This data model supports constitutional DDD principles, Contact Intelligence priority, and Human-AI collaboration workflows.**
