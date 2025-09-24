-- NorthStar Funding Discovery Database Initialization
-- PostgreSQL 16 - Mac Studio Deployment
-- Constitutional Compliance: Contact Intelligence Encryption, DDD Structure

-- Create additional databases for different environments
CREATE DATABASE northstar_funding_dev OWNER northstar_user;
CREATE DATABASE northstar_funding_test OWNER northstar_user;

-- Connect to main database
\c northstar_funding;

-- Create extensions for PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Set timezone to UTC (Constitutional Requirement)
SET timezone = 'UTC';

-- =========================================
-- ENUMS AND CUSTOM TYPES
-- =========================================

-- Candidate Status Enum (Domain-Driven Design)
CREATE TYPE candidate_status AS ENUM (
    'PENDING_REVIEW',
    'IN_REVIEW', 
    'APPROVED',
    'REJECTED'
);

-- Contact Type Enum (Contact Intelligence Priority)
CREATE TYPE contact_type AS ENUM (
    'PROGRAM_OFFICER',
    'FOUNDATION_STAFF',
    'GOVERNMENT_OFFICIAL',
    'ACADEMIC_CONTACT',
    'CORPORATE_CONTACT'
);

-- Authority Level Enum 
CREATE TYPE authority_level AS ENUM (
    'DECISION_MAKER',
    'INFLUENCER', 
    'INFORMATION_ONLY'
);

-- Session Type Enum
CREATE TYPE session_type AS ENUM (
    'SCHEDULED',
    'MANUAL',
    'RETRY'
);

-- Session Status Enum
CREATE TYPE session_status AS ENUM (
    'RUNNING',
    'COMPLETED',
    'FAILED',
    'CANCELLED'
);

-- Enhancement Type Enum
CREATE TYPE enhancement_type AS ENUM (
    'CONTACT_ADDED',
    'DATA_CORRECTED',
    'NOTES_ADDED',
    'DUPLICATE_MERGED'
);

-- Admin Role Enum
CREATE TYPE admin_role AS ENUM (
    'ADMINISTRATOR',
    'REVIEWER'
);

-- =========================================
-- CORE TABLES
-- =========================================

-- Admin Users Table
CREATE TABLE admin_users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role admin_role NOT NULL DEFAULT 'REVIEWER',
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    
    -- Statistics
    candidates_reviewed INTEGER NOT NULL DEFAULT 0,
    average_review_time_minutes INTEGER,
    approval_rate DECIMAL(5,2),
    specializations JSONB DEFAULT '[]'::JSONB
);

-- Discovery Sessions Table (Audit Trail)
CREATE TABLE discovery_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    session_type session_type NOT NULL DEFAULT 'SCHEDULED',
    duration_minutes INTEGER,
    status session_status NOT NULL DEFAULT 'RUNNING',
    
    -- Search Configuration
    search_engines_used JSONB DEFAULT '[]'::JSONB,
    search_queries JSONB DEFAULT '[]'::JSONB,
    query_generation_prompt TEXT,
    
    -- Results Summary
    candidates_found INTEGER DEFAULT 0,
    duplicates_detected INTEGER DEFAULT 0,
    average_confidence_score DECIMAL(4,3),
    error_messages JSONB DEFAULT '[]'::JSONB,
    sources_scraped INTEGER DEFAULT 0
);

-- Funding Source Candidates Table (Aggregate Root)
CREATE TABLE funding_source_candidates (
    candidate_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status candidate_status NOT NULL DEFAULT 'PENDING_REVIEW',
    confidence_score DECIMAL(4,3) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    
    -- Discovery Metadata
    discovery_session_id UUID NOT NULL REFERENCES discovery_sessions(session_id),
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Assignment and Review
    assigned_reviewer_id UUID REFERENCES admin_users(user_id),
    review_started_at TIMESTAMPTZ,
    
    -- Funding Source Data
    organization_name VARCHAR(255) NOT NULL,
    program_name VARCHAR(500) NOT NULL,
    source_url TEXT NOT NULL,
    description TEXT,
    funding_amount_min DECIMAL(12,2),
    funding_amount_max DECIMAL(12,2),
    currency VARCHAR(3),
    geographic_eligibility JSONB DEFAULT '[]'::JSONB,
    organization_types JSONB DEFAULT '[]'::JSONB,
    application_deadline DATE,
    application_process TEXT,
    requirements JSONB DEFAULT '[]'::JSONB,
    tags JSONB DEFAULT '[]'::JSONB,
    
    -- Discovery Metadata
    discovery_method VARCHAR(100) NOT NULL,
    search_query TEXT NOT NULL,
    extracted_data JSONB DEFAULT '{}'::JSONB,
    duplicate_of_candidate_id UUID REFERENCES funding_source_candidates(candidate_id),
    
    -- Enhancement Tracking
    validation_notes TEXT,
    rejection_reason TEXT
);

-- Contact Intelligence Table (First-Class Entity)
CREATE TABLE contact_intelligence (
    contact_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID NOT NULL REFERENCES funding_source_candidates(candidate_id) ON DELETE CASCADE,
    
    -- Basic Contact Information
    contact_type contact_type NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    
    -- ENCRYPTED FIELDS (Constitutional Requirement: Contact PII encrypted at rest)
    email_encrypted BYTEA, -- Encrypted email address
    phone_encrypted BYTEA, -- Encrypted phone number
    
    -- Organization Information
    organization VARCHAR(255),
    office_address TEXT,
    communication_preference VARCHAR(100),
    
    -- Contact History and Validation
    last_contacted_at TIMESTAMPTZ,
    response_pattern TEXT,
    referral_source VARCHAR(255),
    validated_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Relationship Context
    decision_authority authority_level NOT NULL DEFAULT 'INFORMATION_ONLY',
    relationship_notes TEXT,
    referral_connections TEXT,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Enhancement Records Table (Value Object for Audit)
CREATE TABLE enhancement_records (
    enhancement_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID NOT NULL REFERENCES funding_source_candidates(candidate_id) ON DELETE CASCADE,
    enhanced_by UUID NOT NULL REFERENCES admin_users(user_id),
    enhanced_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Enhancement Details
    enhancement_type enhancement_type NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    notes TEXT,
    time_spent_minutes INTEGER
);

-- =========================================
-- INDEXES FOR PERFORMANCE
-- =========================================

-- Funding Source Candidates Indexes
CREATE INDEX idx_candidates_status_confidence ON funding_source_candidates (status, confidence_score DESC);
CREATE INDEX idx_candidates_discovered_at ON funding_source_candidates (discovered_at DESC);
CREATE INDEX idx_candidates_organization_program ON funding_source_candidates (organization_name, program_name);
CREATE INDEX idx_candidates_assigned_reviewer ON funding_source_candidates (assigned_reviewer_id);
CREATE INDEX idx_candidates_discovery_session ON funding_source_candidates (discovery_session_id);

-- Contact Intelligence Indexes
CREATE INDEX idx_contacts_candidate_id ON contact_intelligence (candidate_id);
CREATE INDEX idx_contacts_type ON contact_intelligence (contact_type);
CREATE INDEX idx_contacts_validated_at ON contact_intelligence (validated_at);
CREATE INDEX idx_contacts_decision_authority ON contact_intelligence (decision_authority);

-- Discovery Sessions Indexes
CREATE INDEX idx_sessions_executed_at ON discovery_sessions (executed_at DESC);
CREATE INDEX idx_sessions_status ON discovery_sessions (status);

-- Enhancement Records Indexes
CREATE INDEX idx_enhancements_candidate_id ON enhancement_records (candidate_id);
CREATE INDEX idx_enhancements_enhanced_by ON enhancement_records (enhanced_by);
CREATE INDEX idx_enhancements_enhanced_at ON enhancement_records (enhanced_at DESC);

-- Full Text Search Indexes (for funding source search)
CREATE INDEX idx_candidates_search ON funding_source_candidates USING GIN (
    to_tsvector('english', organization_name || ' ' || program_name || ' ' || COALESCE(description, ''))
);

-- =========================================
-- TRIGGERS FOR AUDIT AND UPDATES
-- =========================================

-- Update last_updated_at trigger function
CREATE OR REPLACE FUNCTION update_last_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to funding_source_candidates
CREATE TRIGGER trigger_update_candidates_updated_at
    BEFORE UPDATE ON funding_source_candidates
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_at();

-- Apply trigger to contact_intelligence
CREATE TRIGGER trigger_update_contacts_updated_at
    BEFORE UPDATE ON contact_intelligence
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_at();

-- =========================================
-- SAMPLE DATA FOR DEVELOPMENT
-- =========================================

-- Insert default admin user (Kevin)
INSERT INTO admin_users (username, full_name, email, role, password_hash) VALUES
('kevin', 'Kevin Administrator', 'kevin@northstar-foundation.org', 'ADMINISTRATOR', '$2a$10$placeholder_password_hash'),
('huw', 'Huw Reviewer', 'huw@northstar-foundation.org', 'REVIEWER', '$2a$10$placeholder_password_hash');

-- =========================================
-- GRANTS AND PERMISSIONS
-- =========================================

-- Grant all permissions to northstar_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO northstar_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO northstar_user;
GRANT USAGE ON SCHEMA public TO northstar_user;

-- Grant usage on custom types
GRANT USAGE ON TYPE candidate_status TO northstar_user;
GRANT USAGE ON TYPE contact_type TO northstar_user;
GRANT USAGE ON TYPE authority_level TO northstar_user;
GRANT USAGE ON TYPE session_type TO northstar_user;
GRANT USAGE ON TYPE session_status TO northstar_user;
GRANT USAGE ON TYPE enhancement_type TO northstar_user;
GRANT USAGE ON TYPE admin_role TO northstar_user;

-- =========================================
-- CONSTITUTIONAL VERIFICATION COMMENTS
-- =========================================

-- ✅ Contact Intelligence Priority: 
--    contact_intelligence table is first-class entity with encrypted PII
--
-- ✅ Domain-Driven Design: 
--    "Funding Sources" terminology, proper aggregate boundaries
--
-- ✅ Human-AI Collaboration:
--    admin_users table for human validation, audit trails preserved
--
-- ✅ Complexity Management:
--    Clean schema design, appropriate indexes, clear relationships

COMMIT;
