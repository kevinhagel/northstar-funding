-- V11__create_search_session_statistics_table.sql
-- Feature: 003-search-execution-infrastructure
-- Purpose: Create search_session_statistics table for tracking search engine performance
-- Constitutional Compliance: PostgreSQL 16, analytics for human review

-- Create search_session_statistics table
CREATE TABLE search_session_statistics (
    id                      BIGSERIAL PRIMARY KEY,
    session_id              UUID NOT NULL,
    engine_type             VARCHAR(50) NOT NULL,
    queries_executed        INTEGER NOT NULL DEFAULT 0,
    results_returned        INTEGER NOT NULL DEFAULT 0,
    avg_response_time_ms    BIGINT NOT NULL DEFAULT 0,
    failure_count           INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key to discovery_session (references session_id UUID column)
    CONSTRAINT fk_search_session_statistics_session
        FOREIGN KEY (session_id)
        REFERENCES discovery_session(session_id)
        ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_search_session_statistics_queries_executed CHECK (queries_executed >= 0),
    CONSTRAINT chk_search_session_statistics_results_returned CHECK (results_returned >= 0),
    CONSTRAINT chk_search_session_statistics_avg_response_time CHECK (avg_response_time_ms >= 0),
    CONSTRAINT chk_search_session_statistics_failure_count CHECK (failure_count >= 0),
    CONSTRAINT chk_search_session_statistics_engine_type CHECK (
        engine_type IN ('SEARXNG', 'BROWSERBASE', 'TAVILY', 'PERPLEXITY')
    )
);

-- Create indexes for analytics queries
CREATE INDEX idx_session_stats_session_id ON search_session_statistics(session_id);
CREATE INDEX idx_session_stats_engine ON search_session_statistics(engine_type);
CREATE INDEX idx_session_stats_created_at ON search_session_statistics(created_at DESC);

-- Composite index for session + engine queries (common analytics pattern)
CREATE INDEX idx_session_stats_session_engine ON search_session_statistics(session_id, engine_type);

-- Add comments for documentation
COMMENT ON TABLE search_session_statistics IS 'Performance statistics per search engine per discovery session. Enables Kevin to identify most productive engines/queries.';
COMMENT ON COLUMN search_session_statistics.session_id IS 'Foreign key to discovery_session.id';
COMMENT ON COLUMN search_session_statistics.engine_type IS 'Search engine type (enum: SEARXNG, BROWSERBASE, TAVILY, PERPLEXITY)';
COMMENT ON COLUMN search_session_statistics.queries_executed IS 'Number of queries executed against this engine in this session';
COMMENT ON COLUMN search_session_statistics.results_returned IS 'Total number of results returned by this engine';
COMMENT ON COLUMN search_session_statistics.avg_response_time_ms IS 'Average response time in milliseconds for this engine';
COMMENT ON COLUMN search_session_statistics.failure_count IS 'Number of failed requests (circuit breaker trips, timeouts, errors)';
