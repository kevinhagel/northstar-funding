-- V16: Create funding_program table
--
-- Represents specific funding opportunities hosted by organizations.
-- Multiple programs can exist under same organization/domain.
--
-- Deduplication Logic:
-- - Same domain + same URL + same day = Skip (duplicate)
-- - Same domain + different URL = Process (different program)
--
-- Hierarchy: Organization → Domain → FundingPrograms → URLs

CREATE TABLE funding_program (
    program_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Parent Relationships
    organization_id UUID NOT NULL REFERENCES organization(organization_id),
    domain VARCHAR(255) NOT NULL REFERENCES domain(domain_name),

    -- Program Details
    program_name VARCHAR(500) NOT NULL,
    program_url VARCHAR(2000) NOT NULL,
    description TEXT,

    -- Eligibility & Requirements
    eligibility_criteria TEXT,
    geographic_scope TEXT,
    funding_amount VARCHAR(200),

    -- Deadlines & Timing
    application_deadline TIMESTAMP,
    funding_period TIMESTAMP,
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern VARCHAR(100),

    -- Discovery Metadata
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discovery_session_id UUID REFERENCES discovery_session(session_id),
    search_result_id UUID, -- FK to search_result (to be created in V17)
    program_confidence DECIMAL(3,2) CHECK (program_confidence BETWEEN 0.00 AND 1.00),
    is_valid_funding_opportunity BOOLEAN,

    -- Status & Tracking
    status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',
    last_refreshed_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    candidate_id UUID REFERENCES funding_source_candidate(candidate_id),

    -- Admin Notes
    notes TEXT,

    -- Constraints
    CONSTRAINT funding_program_url_unique UNIQUE (program_url),
    CONSTRAINT funding_program_domain_url_unique UNIQUE (domain, program_url),
    CONSTRAINT funding_program_status_check CHECK (
        status IN ('DISCOVERED', 'PENDING_JUDGMENT', 'ACTIVE', 'REJECTED', 'EXPIRED', 'ARCHIVED', 'SUSPENDED')
    )
);

-- Indexes for funding_program table
CREATE INDEX idx_funding_program_organization ON funding_program(organization_id);
CREATE INDEX idx_funding_program_domain ON funding_program(domain);
CREATE INDEX idx_funding_program_discovered_at ON funding_program(discovered_at DESC);
CREATE INDEX idx_funding_program_status ON funding_program(status);
CREATE INDEX idx_funding_program_is_valid ON funding_program(is_valid_funding_opportunity) WHERE is_valid_funding_opportunity = true;
CREATE INDEX idx_funding_program_deadline ON funding_program(application_deadline);
CREATE INDEX idx_funding_program_discovery_session ON funding_program(discovery_session_id);
CREATE INDEX idx_funding_program_confidence ON funding_program(program_confidence DESC);

-- Comments
COMMENT ON TABLE funding_program IS 'Specific funding opportunities from organizations (program-level judging)';
COMMENT ON COLUMN funding_program.program_confidence IS 'Confidence score from program-level metadata judging (0.00-1.00)';
COMMENT ON COLUMN funding_program.is_valid_funding_opportunity IS 'Result of program-level judgment';
COMMENT ON COLUMN funding_program.status IS 'Lifecycle state: DISCOVERED, PENDING_JUDGMENT, ACTIVE, REJECTED, EXPIRED, ARCHIVED, SUSPENDED';
COMMENT ON COLUMN funding_program.is_recurring IS 'Does this program recur annually/quarterly?';
