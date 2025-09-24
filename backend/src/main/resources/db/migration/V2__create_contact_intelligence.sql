-- V2: Create ContactIntelligence Table (First-Class Entity)
-- Constitutional Requirement: Contact Intelligence as highest value asset
-- PII Protection: Encrypted storage for email/phone (Constitutional mandate)
-- Relationship Tracking: Decision authority and referral networks

-- Drop table if exists (for development)
DROP TABLE IF EXISTS contact_intelligence CASCADE;

-- Create ENUM types for Contact Intelligence domain
CREATE TYPE contact_type AS ENUM (
    'PROGRAM_OFFICER',
    'FOUNDATION_STAFF', 
    'GOVERNMENT_OFFICIAL',
    'ACADEMIC_CONTACT',
    'CORPORATE_CONTACT'
);

CREATE TYPE authority_level AS ENUM (
    'DECISION_MAKER',
    'INFLUENCER',
    'INFORMATION_ONLY'
);

-- ContactIntelligence: First-class entity for relationship management
CREATE TABLE contact_intelligence (
    -- Primary Key & Relationships
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL, -- FK to FundingSourceCandidate
    
    -- Contact Classification
    contact_type contact_type NOT NULL,
    authority_level authority_level NOT NULL DEFAULT 'INFORMATION_ONLY',
    
    -- Personal Information
    full_name VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    organization VARCHAR(300),
    office_address TEXT,
    
    -- Protected Contact Information (ENCRYPTED - Constitutional Requirement)
    -- Note: Encryption will be handled at application layer with @ColumnTransformer
    email VARCHAR(500), -- Will be encrypted in application layer
    phone VARCHAR(100), -- Will be encrypted in application layer
    
    -- Communication Preferences & Patterns
    communication_preference VARCHAR(100) DEFAULT 'email',
    last_contacted_at TIMESTAMPTZ NULL,
    response_pattern TEXT,
    referral_source TEXT,
    
    -- Validation & Status
    validated_at TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Relationship Context (High-Value Intelligence)
    relationship_notes TEXT,
    referral_connections TEXT, -- JSON-like structure for referral chains
    
    -- Audit Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, -- FK to AdminUser (will be added in V3)
    
    -- Foreign Key Constraints
    CONSTRAINT contact_intelligence_candidate_fk 
        FOREIGN KEY (candidate_id) 
        REFERENCES funding_source_candidate(candidate_id) 
        ON DELETE CASCADE,
        
    -- Business Rules
    CONSTRAINT contact_intelligence_email_or_phone_required
        CHECK (email IS NOT NULL OR phone IS NOT NULL),
        
    CONSTRAINT contact_intelligence_validated_when_contacted
        CHECK (
            (last_contacted_at IS NOT NULL AND validated_at IS NOT NULL) OR
            (last_contacted_at IS NULL)
        )
);

-- Indexes for Performance & Relationship Queries
CREATE INDEX idx_contact_intelligence_candidate_id 
    ON contact_intelligence (candidate_id);
    
CREATE INDEX idx_contact_intelligence_contact_type 
    ON contact_intelligence (contact_type);
    
CREATE INDEX idx_contact_intelligence_authority_level 
    ON contact_intelligence (authority_level);
    
CREATE INDEX idx_contact_intelligence_organization 
    ON contact_intelligence (organization);
    
CREATE INDEX idx_contact_intelligence_validation_status 
    ON contact_intelligence (validated_at DESC NULLS LAST) 
    WHERE is_active = true;

-- Full-text search for contact information
CREATE INDEX idx_contact_intelligence_search 
    ON contact_intelligence 
    USING gin(to_tsvector('english', 
        full_name || ' ' || 
        COALESCE(title, '') || ' ' || 
        COALESCE(organization, '') || ' ' ||
        COALESCE(relationship_notes, '')
    ));

-- Relationship network queries (find connections)
CREATE INDEX idx_contact_intelligence_referral_connections 
    ON contact_intelligence (referral_connections) 
    WHERE referral_connections IS NOT NULL;

-- Comments for Domain Understanding
COMMENT ON TABLE contact_intelligence IS 'First-class entity for contact relationship management. Highest value asset in funding discovery process.';
COMMENT ON COLUMN contact_intelligence.email IS 'Encrypted at application layer - Constitutional PII protection requirement';
COMMENT ON COLUMN contact_intelligence.phone IS 'Encrypted at application layer - Constitutional PII protection requirement';
COMMENT ON COLUMN contact_intelligence.authority_level IS 'Decision-making power: DECISION_MAKER > INFLUENCER > INFORMATION_ONLY';
COMMENT ON COLUMN contact_intelligence.relationship_notes IS 'High-value intelligence: interaction history, preferences, relationship context';
COMMENT ON COLUMN contact_intelligence.referral_connections IS 'Network mapping: mutual connections, referral chains for relationship building';
COMMENT ON COLUMN contact_intelligence.validated_at IS 'When contact information was last verified as accurate';

-- Constitutional Compliance Verification  
-- ✅ Contact Intelligence as first-class entity (not embedded in aggregate)
-- ✅ PII encryption preparation (email/phone fields marked for application-layer encryption)
-- ✅ Relationship tracking and referral network support
-- ✅ Authority level classification for decision-making context
-- ✅ Performance indexes for contact queries and relationship mapping
-- ✅ Cascade delete maintains aggregate consistency with FundingSourceCandidate
-- ✅ Full-text search for intelligence mining
