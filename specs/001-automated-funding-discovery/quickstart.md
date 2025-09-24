# Quickstart Test Scenarios

**Feature**: Automated Funding Discovery Workflow  
**Date**: 2025-09-22  
**Purpose**: Integration test scenarios for validating end-to-end admin workflows

## Overview

These scenarios validate the complete admin workflow from funding discovery to approval, ensuring human-AI collaboration patterns work correctly and constitutional principles are followed.

## Prerequisites

### System Setup
- Mac Studio (192.168.1.10) running PostgreSQL 16
- NorthStar Funding Discovery service running on port 8080
- Streamlit admin interface running on port 8501
- LM Studio running locally for AI enhancement features
- Test admin users: Kevin (admin), Huw (reviewer)

### Test Data Setup
```bash
# Create test admin users
curl -X POST http://192.168.1.10:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "kevin",
    "fullName": "Kevin Administrator", 
    "email": "kevin@northstar-foundation.org",
    "role": "ADMINISTRATOR"
  }'

curl -X POST http://192.168.1.10:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "huw", 
    "fullName": "Huw Reviewer",
    "email": "huw@northstar-foundation.org",
    "role": "REVIEWER"
  }'
```

## Test Scenario 1: Complete Discovery to Approval Workflow

### User Story
**As Kevin (admin user)**, I want to see newly discovered funding source candidates, review and enhance them with contact intelligence, and approve them for the knowledge base.

### Test Steps

#### Step 1: Trigger Discovery Session
```bash
# Manual discovery trigger
curl -X POST http://192.168.1.10:8080/api/discovery/trigger \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "searchEngines": ["searxng", "tavily"],
    "targetRegions": ["Bulgaria", "Eastern Europe"],
    "customQueries": ["education grants Bulgaria", "school funding Eastern Europe"]
  }'
```

**Expected Result**: 
- HTTP 202 response with session ID
- Discovery session starts and finds 5-10 candidates
- Session completes within 10 minutes

#### Step 2: Review Candidate Queue
```bash
# Get pending candidates ordered by confidence score
curl -X GET "http://192.168.1.10:8080/api/candidates?status=PENDING_REVIEW&sortBy=confidenceScore&sortDirection=desc" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="
```

**Expected Result**:
- List of candidates with confidence scores 0.3-0.9
- Each candidate has: organizationName, programName, sourceUrl, discoveredAt
- At least one candidate with high confidence (>0.7)

#### Step 3: Review High-Confidence Candidate
```bash
# Get detailed candidate information
CANDIDATE_ID="[UUID from step 2]"
curl -X GET "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="
```

**Expected Result**:
- Full candidate details including extracted data
- Discovery metadata (search query, method, source URL)
- Status: PENDING_REVIEW
- No contact intelligence initially

#### Step 4: Enhance Candidate with Manual Data
```bash
# Update candidate with enhanced information
curl -X PUT "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "description": "Enhanced description after manual website review",
    "fundingAmountMin": 5000,
    "fundingAmountMax": 50000,
    "currency": "EUR",
    "geographicEligibility": ["Bulgaria", "Romania", "Eastern Europe"],
    "organizationTypes": ["Educational Institutions", "NGOs"],
    "applicationDeadline": "2025-12-31",
    "applicationProcess": "Online application through foundation portal",
    "requirements": ["Registered educational institution", "Minimum 2 years operation", "Student count >50"],
    "validationNotes": "Verified website active, program confirmed for 2025"
  }'
```

**Expected Result**:
- HTTP 200 response with updated candidate
- Enhancement record created in audit trail
- lastUpdatedAt timestamp updated

#### Step 5: Add Contact Intelligence
```bash
# Add program officer contact
curl -X POST "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/contacts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "contactType": "PROGRAM_OFFICER",
    "fullName": "Maria Petrova",
    "title": "Education Program Manager", 
    "email": "m.petrova@foundation-example.org",
    "phone": "+359-2-123-4567",
    "organization": "Bulgarian Education Foundation",
    "officeAddress": "Sofia, Bulgaria",
    "communicationPreference": "Email preferred, English/Bulgarian",
    "decisionAuthority": "DECISION_MAKER",
    "relationshipNotes": "Primary contact for education grants, responds within 48 hours",
    "referralSource": "Found on foundation website staff directory"
  }'
```

**Expected Result**:
- HTTP 201 response with contact ID
- Contact intelligence encrypted at rest (email/phone)
- Contact associated with candidate

#### Step 6: Approve Candidate
```bash
# Approve candidate for knowledge base
curl -X POST "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "approvalNotes": "High-quality candidate with verified contact intelligence. Excellent fit for Bulgarian education sector."
  }'
```

**Expected Result**:
- HTTP 200 response confirming approval
- Candidate status changed to APPROVED
- Available in searchable knowledge base

### Validation Criteria
- ✅ Discovery finds realistic funding sources
- ✅ Human enhancement workflow functional
- ✅ Contact intelligence properly encrypted/stored
- ✅ Approval workflow moves candidate to knowledge base
- ✅ Audit trail preserved for all changes
- ✅ Performance: Each API call <500ms

## Test Scenario 2: Duplicate Detection and Rejection Workflow

### User Story
**As Huw (reviewer)**, I want to identify duplicate candidates from multiple discovery sessions and reject low-quality candidates to maintain database quality.

### Test Steps

#### Step 1: Run Second Discovery Session
```bash
# Trigger another discovery with overlapping queries
curl -X POST http://192.168.1.10:8080/api/discovery/trigger \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic aHV3OnBhc3N3b3Jk" \
  -d '{
    "searchEngines": ["perplexity", "searxng"],
    "targetRegions": ["Bulgaria"],  
    "customQueries": ["Bulgarian education funding", "school grants Bulgaria"]
  }'
```

#### Step 2: Identify Duplicates
```bash
# Get candidates to find duplicates
curl -X GET "http://192.168.1.10:8080/api/candidates?status=PENDING_REVIEW&page=0&size=50" \
  -H "Authorization: Basic aHV3OnBhc3N3b3Jk"
```

**Expected Behavior**: System should detect duplicates based on organizationName + programName and mark with duplicateOfCandidateId reference.

#### Step 3: Reject Low-Quality Candidate
```bash
# Find low confidence candidate (score < 0.4)
LOW_CONFIDENCE_ID="[UUID of candidate with confidence < 0.4]"

curl -X POST "http://192.168.1.10:8080/api/candidates/$LOW_CONFIDENCE_ID/reject" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic aHV3OnBhc3N3b3Jk" \
  -d '{
    "rejectionReason": "Low confidence score (0.3), incomplete organization information, broken source URL"
  }'
```

**Expected Result**:
- HTTP 200 response  
- Candidate status changed to REJECTED
- Rejection reason stored for analysis

### Validation Criteria
- ✅ Duplicate detection identifies same organization+program
- ✅ Rejection workflow removes candidates from queue
- ✅ Rejection reasons captured for improvement
- ✅ Quality control maintains database standards

## Test Scenario 3: AI-Assisted Enhancement Workflow

### User Story  
**As Kevin (admin user)**, I want to use AI tools to help gather additional information from funding source websites to speed up the enhancement process.

### Test Steps

#### Step 1: Select Candidate for AI Enhancement
```bash
# Get a candidate with missing information
curl -X GET "http://192.168.1.10:8080/api/candidates?status=PENDING_REVIEW&minConfidence=0.5" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="

# Select candidate with incomplete data
CANDIDATE_ID="[UUID of candidate missing contact information]"
```

#### Step 2: Use AI Enhancement Service
```bash
# Trigger AI-assisted data enhancement
curl -X POST "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/enhance" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "enhancementType": "CONTACT_EXTRACTION",
    "sourceUrl": "[candidate source URL]",
    "aiPrompt": "Extract contact information from this funding program page, look for program officers, email addresses, and phone numbers"
  }'
```

**Expected Result**:
- AI service analyzes source webpage via LM Studio
- Extracts potential contact information
- Returns structured suggestions for admin review

#### Step 3: Review and Apply AI Suggestions
```bash
# Get AI enhancement suggestions
curl -X GET "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/enhancements" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="
```

**Expected Response**:
```json
{
  "suggestions": [
    {
      "type": "CONTACT_FOUND",
      "confidence": 0.8,
      "data": {
        "name": "Dr. Elena Dimitrova",
        "title": "Program Director",
        "email": "e.dimitrova@foundation.bg",
        "source": "Contact page footer"
      }
    }
  ]
}
```

#### Step 4: Apply Selected Suggestions
```bash
# Admin applies AI suggestions after validation
curl -X POST "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/contacts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ=" \
  -d '{
    "contactType": "PROGRAM_OFFICER",
    "fullName": "Dr. Elena Dimitrova",
    "title": "Program Director",
    "email": "e.dimitrova@foundation.bg",
    "decisionAuthority": "DECISION_MAKER",
    "relationshipNotes": "AI-extracted contact, needs verification"
  }'
```

### Validation Criteria
- ✅ AI enhancement integrates with LM Studio
- ✅ Structured suggestions returned for review
- ✅ Human validation required before applying
- ✅ AI suggestions improve data completeness
- ✅ Constitutional human-AI collaboration respected

## Test Scenario 4: Audit Trail and Enhancement Tracking

### User Story
**As Kevin (admin user)**, I want to track all changes made to candidates for quality improvement and audit purposes.

### Test Steps

#### Step 1: Review Enhancement History
```bash
# Get enhancement audit trail for candidate
curl -X GET "http://192.168.1.10:8080/api/candidates/$CANDIDATE_ID/enhancements" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="
```

**Expected Result**: Complete audit trail showing:
- Original discovered data vs current data
- All manual enhancements with timestamps
- Admin user who made each change
- Time spent on enhancements

#### Step 2: Discovery Session Analytics
```bash
# Get discovery session performance metrics
curl -X GET "http://192.168.1.10:8080/api/discovery/sessions?page=0&size=10" \
  -H "Authorization: Basic a2V2aW46cGFzc3dvcmQ="
```

**Expected Result**: Session analytics showing:
- Candidates found per session
- Average confidence scores
- Success/failure rates by search engine
- Processing time metrics

### Validation Criteria
- ✅ Complete audit trail for all changes
- ✅ Performance metrics for discovery improvement
- ✅ Quality tracking shows enhancement effectiveness
- ✅ Constitutional requirement for learning capture met

## Performance Benchmarks

### Response Time Requirements
- Candidate listing: <500ms for 20 candidates
- Candidate details: <200ms  
- Candidate update: <300ms
- Discovery session trigger: <100ms (async)
- Contact intelligence queries: <200ms

### Throughput Requirements  
- Support 2 concurrent admin users
- Process 1000 candidates per day
- Handle 5 discovery sessions per day
- Store 10K approved funding sources

### Data Quality Requirements
- Contact intelligence accuracy >90% after human validation
- Duplicate detection rate >95% for same organization+program
- AI enhancement suggestions accuracy >70%

## Failure Scenarios

### Discovery Service Failures
- **LM Studio unavailable**: Discovery continues with reduced AI query generation
- **Search engine timeout**: Partial results returned, session marked as warning
- **Database connection loss**: Discovery session paused, resumed when connection restored

### Human Workflow Failures  
- **Admin user session timeout**: Work preserved, can resume where left off
- **Invalid enhancement data**: Validation errors returned, no partial updates
- **Contact intelligence validation failure**: Contact marked for manual verification

## Success Criteria Summary

**Functional Requirements Met**:
- ✅ FR-001: Automated discovery execution verified
- ✅ FR-002: Confidence scoring and candidate extraction working  
- ✅ FR-003: Pending validation status workflow functional
- ✅ FR-004: Admin review queue interface operational
- ✅ FR-005: AI-assisted enhancement tools integrated
- ✅ FR-006: Manual enhancement capabilities confirmed
- ✅ FR-007: Approval workflow moves candidates to knowledge base
- ✅ FR-008: Rejection workflow removes candidates from queue
- ✅ FR-009: Duplicate detection prevents database pollution
- ✅ FR-010: Discovery audit trail captured
- ✅ FR-011: Original vs enhanced data preserved
- ✅ FR-012: Bulk operations support (future feature)

**Constitutional Compliance**:
- ✅ Human-AI collaboration: Human validation required for all approvals
- ✅ Contact intelligence priority: Contacts encrypted and relationship-tracked  
- ✅ DDD ubiquitous language: "Funding Sources" terminology throughout
- ✅ Complexity management: Single service architecture maintained

**Ready for Production**: All quickstart scenarios must pass before deployment to Mac Studio.
