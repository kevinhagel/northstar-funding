-- V9: Update candidate_status for two-phase processing pipeline
--
-- Two-Phase Processing:
-- Phase 1 (Metadata Judging): PENDING_CRAWL - judge based on search metadata only
-- Phase 2 (Deep Crawling): PENDING_REVIEW - high-confidence candidates crawled
--
-- New Statuses:
-- - PENDING_CRAWL: High-confidence candidate pending deep web crawl
-- - SKIPPED_LOW_CONFIDENCE: Low confidence from Phase 1, not worth crawling

-- Drop old status constraint
ALTER TABLE funding_source_candidate
DROP CONSTRAINT IF EXISTS funding_source_candidate_status_check;

-- Add new status constraint with two-phase processing statuses
ALTER TABLE funding_source_candidate
ADD CONSTRAINT funding_source_candidate_status_check
    CHECK (status IN (
        'PENDING_CRAWL',
        'PENDING_REVIEW',
        'IN_REVIEW',
        'APPROVED',
        'REJECTED',
        'SKIPPED_LOW_CONFIDENCE'
    ));

-- Update default status to PENDING_CRAWL (Phase 1 entry point)
ALTER TABLE funding_source_candidate
ALTER COLUMN status SET DEFAULT 'PENDING_CRAWL';

-- Comments for documentation
COMMENT ON COLUMN funding_source_candidate.status IS 'Two-phase workflow: PENDING_CRAWL (Phase 1: metadata judging) → PENDING_REVIEW (Phase 2: crawled) → IN_REVIEW → APPROVED/REJECTED. SKIPPED_LOW_CONFIDENCE for low-quality candidates.';
