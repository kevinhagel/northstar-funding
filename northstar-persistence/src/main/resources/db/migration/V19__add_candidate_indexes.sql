-- V19: Add performance indexes for admin dashboard review queue
-- Feature 013: Admin Dashboard Review Queue
-- Indexes support filtering and sorting operations on funding_source_candidate table

-- Note: idx_candidate_search_engine already exists in V1, so we skip it here

-- Status filter (multi-select dropdown)
CREATE INDEX IF NOT EXISTS idx_candidate_status
    ON funding_source_candidate(status);

-- Confidence score range filter (slider)
CREATE INDEX IF NOT EXISTS idx_candidate_confidence
    ON funding_source_candidate(confidence_score);

-- Discovered date sorting (default sort: newest first) and date range filter
CREATE INDEX IF NOT EXISTS idx_candidate_discovered_at
    ON funding_source_candidate(discovered_at);

-- Composite index for common query pattern: status filter + confidence sort
CREATE INDEX IF NOT EXISTS idx_candidate_status_confidence
    ON funding_source_candidate(status, confidence_score);

-- Composite index for common query pattern: date range + confidence sort
CREATE INDEX IF NOT EXISTS idx_candidate_discovered_confidence
    ON funding_source_candidate(discovered_at DESC, confidence_score DESC);
