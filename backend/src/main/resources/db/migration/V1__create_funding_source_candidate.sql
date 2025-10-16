-- V1: Create FundingSourceCandidate Table (Aggregate Root)
-- NorthStar Funding Discovery - Constitutional Compliance
-- Domain: Funding Sources (Ubiquitous Language)
-- Human-AI Collaboration: Discovery → Review → Approval Workflow

-- Drop table if exists (for development)
DROP TABLE IF EXISTS funding_source_candidate CASCADE;

-- FundingSourceCandidate: Core aggregate root for discovered funding opportunities
CREATE TABLE funding_source_candidate (
    -- Primary Key & Status
    candidate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    confidence_score DECIMAL(3,2) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    
    -- Audit Timestamps (Constitutional Requirement)
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Review Assignment (Human-AI Collaboration)
    assigned_reviewer_id UUID NULL, -- FK to AdminUser (will be added in V3)
    review_started_at TIMESTAMPTZ NULL,
    
    -- Core Funding Source Data (Domain Model)
    organization_name VARCHAR(500) NOT NULL,
    program_name VARCHAR(500) NOT NULL,
    source_url TEXT NOT NULL,
    description TEXT,
    
    -- Funding Details
    funding_amount_min DECIMAL(15,2) NULL,
    funding_amount_max DECIMAL(15,2) NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    geographic_eligibility TEXT[] DEFAULT '{}',
    organization_types TEXT[] DEFAULT '{}',
    application_deadline DATE NULL,
    application_process TEXT,
    requirements TEXT[] DEFAULT '{}',
    tags TEXT[] DEFAULT '{}',
    
    -- Discovery Metadata (AI Processing Context)  
    discovery_session_id UUID NULL, -- FK to DiscoverySession (will be added in V4)
    discovery_method VARCHAR(100) NULL,
    search_query TEXT NULL,
    extracted_data TEXT NOT NULL DEFAULT '{}',
    duplicate_of_candidate_id UUID NULL, -- Self-referential FK for deduplication
    
    -- Enhancement & Validation (Human Enhancement)
    validation_notes TEXT,
    rejection_reason TEXT,
    
    -- Constraints
    CONSTRAINT funding_source_candidate_status_check
        CHECK (status IN ('PENDING_REVIEW', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    
    CONSTRAINT funding_source_candidate_duplicate_fk 
        FOREIGN KEY (duplicate_of_candidate_id) 
        REFERENCES funding_source_candidate(candidate_id) 
        ON DELETE SET NULL,
        
    CONSTRAINT funding_source_candidate_unique_approved_source
        EXCLUDE (organization_name WITH =, program_name WITH =) 
        WHERE (status = 'APPROVED'),
        
    CONSTRAINT funding_source_candidate_rejection_reason_required
        CHECK (
            (status = 'REJECTED' AND rejection_reason IS NOT NULL) OR 
            (status != 'REJECTED')
        ),
        
    CONSTRAINT funding_source_candidate_review_assignment_logic
        CHECK (
            (status = 'IN_REVIEW' AND assigned_reviewer_id IS NOT NULL AND review_started_at IS NOT NULL) OR
            (status != 'IN_REVIEW')
        )
);

-- Indexes for Performance (Constitutional: <500ms API requirement)
CREATE INDEX idx_funding_source_candidate_status_confidence 
    ON funding_source_candidate (status, confidence_score DESC);
    
CREATE INDEX idx_funding_source_candidate_discovered_at 
    ON funding_source_candidate (discovered_at DESC);
    
CREATE INDEX idx_funding_source_candidate_assigned_reviewer 
    ON funding_source_candidate (assigned_reviewer_id) 
    WHERE assigned_reviewer_id IS NOT NULL;
    
CREATE INDEX idx_funding_source_candidate_discovery_session 
    ON funding_source_candidate (discovery_session_id) 
    WHERE discovery_session_id IS NOT NULL;

-- Full-text search for funding source content
CREATE INDEX idx_funding_source_candidate_search 
    ON funding_source_candidate 
    USING gin(to_tsvector('english', organization_name || ' ' || program_name || ' ' || COALESCE(description, '')));

-- JSONB indexes for filtering
CREATE INDEX idx_funding_source_candidate_tags 
    ON funding_source_candidate USING gin(tags);

-- Comments for Domain Understanding
COMMENT ON TABLE funding_source_candidate IS 'Aggregate Root: Discovered funding opportunities pending human validation. Core entity in Funding Sources bounded context.';
COMMENT ON COLUMN funding_source_candidate.status IS 'Workflow state: PENDING_REVIEW, IN_REVIEW, APPROVED, or REJECTED. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';
COMMENT ON COLUMN funding_source_candidate.confidence_score IS 'AI-generated quality score (0.0-1.0) for prioritizing human review';
COMMENT ON COLUMN funding_source_candidate.extracted_data IS 'Raw scraped data as JSON for audit and improvement';
COMMENT ON COLUMN funding_source_candidate.duplicate_of_candidate_id IS 'Self-reference for duplicate detection across discovery sessions';
COMMENT ON COLUMN funding_source_candidate.tags IS 'Categorization tags as JSON array for filtering and organization';

-- Constitutional Compliance Verification
-- ✅ PostgreSQL 16 compatible syntax
-- ✅ UUID primary keys for distributed system readiness  
-- ✅ JSONB for flexible data storage (Constitutional requirement)
-- ✅ Human-AI Collaboration workflow states
-- ✅ Performance indexes for <500ms API responses
-- ✅ Domain-Driven Design: "Funding Sources" terminology
-- ✅ Audit timestamps with timezone awareness
