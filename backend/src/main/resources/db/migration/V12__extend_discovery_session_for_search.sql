-- V12__extend_discovery_session_for_search.sql
-- Feature: 003-search-execution-infrastructure
-- Purpose: Extend discovery_session table to support search execution tracking
-- Constitutional Compliance: Backward compatible, update existing rows

-- Add new columns for search execution tracking
ALTER TABLE discovery_session
    ADD COLUMN discovery_type VARCHAR(50),
    ADD COLUMN search_query_set_name VARCHAR(100),
    ADD COLUMN total_search_queries_executed INTEGER DEFAULT 0;

-- Update existing rows to have discovery_type = 'METADATA_JUDGING' (from Feature 002)
UPDATE discovery_session
SET discovery_type = 'METADATA_JUDGING'
WHERE discovery_type IS NULL;

-- Make discovery_type NOT NULL after backfilling
ALTER TABLE discovery_session
    ALTER COLUMN discovery_type SET NOT NULL,
    ALTER COLUMN discovery_type SET DEFAULT 'METADATA_JUDGING';

-- Add constraint for discovery_type enum
ALTER TABLE discovery_session
    ADD CONSTRAINT chk_discovery_session_discovery_type CHECK (
        discovery_type IN ('METADATA_JUDGING', 'SEARCH_EXECUTION')
    );

-- Add constraint for total_search_queries_executed
ALTER TABLE discovery_session
    ADD CONSTRAINT chk_discovery_session_total_queries CHECK (
        total_search_queries_executed >= 0
    );

-- Create index for discovery_type queries
CREATE INDEX idx_discovery_session_discovery_type ON discovery_session(discovery_type);

-- Add comments for new columns
COMMENT ON COLUMN discovery_session.discovery_type IS 'Type of discovery session: METADATA_JUDGING (Feature 002) or SEARCH_EXECUTION (Feature 003)';
COMMENT ON COLUMN discovery_session.search_query_set_name IS 'Name of query set used (e.g., "monday-queries", "tuesday-queries") - NULL for METADATA_JUDGING sessions';
COMMENT ON COLUMN discovery_session.total_search_queries_executed IS 'Total number of search queries executed in this session (0 for METADATA_JUDGING sessions)';
