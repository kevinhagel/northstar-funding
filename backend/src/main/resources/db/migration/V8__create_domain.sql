-- V8: Create domain table for domain-level deduplication and blacklisting
--
-- Constitutional Principles:
-- - Domain-level deduplication (not URL-level)
-- - Permanent blacklist storage (PostgreSQL, not Redis with TTL)
-- - "No funds this year" tracking for legitimate funders
--
-- Example: us-bulgaria.org discovered → skip us-bulgaria.org/programs/education

-- Create domain table
CREATE TABLE domain (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',

    -- Discovery and Processing Tracking
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discovery_session_id UUID REFERENCES discovery_session(session_id),
    last_processed_at TIMESTAMP,
    processing_count INTEGER NOT NULL DEFAULT 0,

    -- Quality and Filtering
    best_confidence_score DECIMAL(3,2) CHECK (best_confidence_score >= 0.00 AND best_confidence_score <= 1.00),
    high_quality_candidate_count INTEGER NOT NULL DEFAULT 0,
    low_quality_candidate_count INTEGER NOT NULL DEFAULT 0,

    -- Blacklist and Administrative Actions
    blacklisted_by UUID REFERENCES admin_user(user_id),
    blacklisted_at TIMESTAMP,
    blacklist_reason TEXT,
    no_funds_year INTEGER,
    notes TEXT,

    -- Error Tracking
    failure_reason TEXT,
    failure_count INTEGER NOT NULL DEFAULT 0,
    retry_after TIMESTAMP,

    -- Status validation
    CONSTRAINT domain_status_check
        CHECK (status IN ('DISCOVERED', 'PROCESSING', 'PROCESSED_HIGH_QUALITY',
                         'PROCESSED_LOW_QUALITY', 'BLACKLISTED', 'NO_FUNDS_THIS_YEAR',
                         'PROCESSING_FAILED'))
);

-- Indexes for common queries
CREATE INDEX idx_domain_name ON domain(domain_name);
CREATE INDEX idx_domain_status ON domain(status);
CREATE INDEX idx_domain_discovered_at ON domain(discovered_at);
CREATE INDEX idx_domain_last_processed ON domain(last_processed_at);

-- Index for finding domains ready for retry after failure
CREATE INDEX idx_domain_retry ON domain(status, retry_after)
    WHERE status = 'PROCESSING_FAILED' AND retry_after IS NOT NULL;

-- Add domain_id foreign key to funding_source_candidate
ALTER TABLE funding_source_candidate
ADD COLUMN domain_id UUID REFERENCES domain(domain_id);

-- Index for candidate-domain relationship
CREATE INDEX idx_candidate_domain ON funding_source_candidate(domain_id);

-- Comments for documentation
COMMENT ON TABLE domain IS 'Domain registry for deduplication and blacklisting';
COMMENT ON COLUMN domain.domain_name IS 'Domain extracted from URL (e.g., us-bulgaria.org)';
COMMENT ON COLUMN domain.status IS 'Current processing status';
COMMENT ON COLUMN domain.best_confidence_score IS 'Highest confidence from any candidate from this domain (0.00-1.00, DECIMAL for precision)';
COMMENT ON COLUMN domain.blacklist_reason IS 'Human-provided reason for blacklisting (e.g., Known scam site)';
COMMENT ON COLUMN domain.no_funds_year IS 'Year when no funds available status was set';
COMMENT ON COLUMN domain.retry_after IS 'When to retry processing after failure (exponential backoff)';
