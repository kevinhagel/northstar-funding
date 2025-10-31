-- Feature 004: AI-Powered Query Generation and Metadata Judging
-- Create query_generation_sessions table for tracking AI query generation statistics
-- NOTE: Must be created BEFORE V14 because search_queries references it

CREATE TABLE query_generation_sessions (
    id                      BIGSERIAL PRIMARY KEY,
    generation_date         DATE NOT NULL,
    ai_model_used           VARCHAR(100) NOT NULL,
    queries_requested       INTEGER NOT NULL,
    queries_generated       INTEGER NOT NULL DEFAULT 0,
    queries_approved        INTEGER NOT NULL DEFAULT 0,
    queries_rejected        INTEGER NOT NULL DEFAULT 0,
    rejection_reasons       TEXT[] NULL,
    generation_duration_ms  BIGINT NOT NULL,
    fallback_used           BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason         TEXT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraint: generated queries must equal approved + rejected
    CONSTRAINT chk_queries_consistency CHECK (
        queries_generated = queries_approved + queries_rejected
    ),
    CONSTRAINT chk_positive_counts CHECK (
        queries_requested > 0 AND
        queries_generated >= 0 AND
        queries_approved >= 0 AND
        queries_rejected >= 0
    )
);

-- Index for querying recent sessions
CREATE INDEX idx_query_gen_date ON query_generation_sessions(generation_date DESC);
CREATE INDEX idx_query_gen_fallback ON query_generation_sessions(fallback_used);

-- Comments for documentation
COMMENT ON TABLE query_generation_sessions IS 'Tracks AI query generation sessions and statistics';
COMMENT ON COLUMN query_generation_sessions.ai_model_used IS 'LM Studio model name (e.g., llama-3.1-8b-instruct)';
COMMENT ON COLUMN query_generation_sessions.rejection_reasons IS 'Array of reasons why queries were rejected (too generic, duplicate, etc.)';
COMMENT ON COLUMN query_generation_sessions.fallback_used IS 'TRUE if LM Studio failed and system fell back to hardcoded queries';
COMMENT ON COLUMN query_generation_sessions.generation_duration_ms IS 'Total time taken for query generation in milliseconds';

-- Add FK constraint to search_queries now that this table exists
ALTER TABLE search_queries
ADD CONSTRAINT fk_search_queries_generation_session
    FOREIGN KEY (generation_session_id)
    REFERENCES query_generation_sessions(id)
    ON DELETE SET NULL;
