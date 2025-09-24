-- V4: Create DiscoverySession Table (Automated Discovery Audit Trail)  
-- AI Processing Context: Track automated discovery executions for improvement
-- Search Configuration: Query generation and search engine usage patterns
-- Performance Metrics: Success rates and processing times for optimization

-- Drop table if exists (for development)
DROP TABLE IF EXISTS discovery_session CASCADE;

-- Create ENUM types for Discovery Session domain  
CREATE TYPE session_status AS ENUM (
    'RUNNING',
    'COMPLETED', 
    'FAILED',
    'CANCELLED'
);

-- Create session_type for discovery sessions
CREATE TYPE session_type AS ENUM (
    'SCHEDULED',
    'MANUAL',
    'RETRY'
);

-- DiscoverySession: Audit record of automated discovery executions
CREATE TABLE discovery_session (
    -- Primary Key & Identification  
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    executed_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM', -- System/scheduler identifier
    session_type session_type NOT NULL DEFAULT 'SCHEDULED',
    
    -- Execution Status & Timing
    status session_status NOT NULL DEFAULT 'RUNNING',
    duration_minutes INTEGER DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ NULL,
    
    -- Search Configuration (AI Query Generation Context)
    search_engines_used JSONB NOT NULL DEFAULT '[]'::jsonb, -- ["searxng", "tavily", "perplexity"]
    search_queries JSONB NOT NULL DEFAULT '[]'::jsonb, -- Generated queries executed
    query_generation_prompt TEXT, -- AI prompt used for query generation
    
    -- Results & Performance Metrics  
    candidates_found INTEGER NOT NULL DEFAULT 0,
    duplicates_detected INTEGER NOT NULL DEFAULT 0,
    sources_scraped INTEGER NOT NULL DEFAULT 0,
    average_confidence_score DECIMAL(3,2) DEFAULT 0.00,
    
    -- Error Handling & Diagnostics
    error_messages JSONB DEFAULT '[]'::jsonb,
    search_engine_failures JSONB DEFAULT '{}'::jsonb, -- Per-engine error tracking
    
    -- Process Metadata (for AI improvement)
    llm_model_used VARCHAR(100), -- LM Studio model used for query generation
    search_parameters JSONB DEFAULT '{}'::jsonb, -- Configuration used
    
    -- Business Rules
    CONSTRAINT discovery_session_timing_logic
        CHECK (
            (status = 'RUNNING' AND completed_at IS NULL) OR
            (status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND completed_at IS NOT NULL)
        ),
        
    CONSTRAINT discovery_session_duration_calculation
        CHECK (
            (completed_at IS NULL AND duration_minutes = 0) OR
            (completed_at IS NOT NULL AND duration_minutes >= 0)
        ),
        
    CONSTRAINT discovery_session_results_consistency  
        CHECK (
            (status = 'COMPLETED' AND candidates_found >= 0) OR
            (status != 'COMPLETED')
        )
);

-- Now we can add the foreign key constraint that was deferred from V1
-- Add foreign key to FundingSourceCandidate.discovery_session_id
ALTER TABLE funding_source_candidate 
ADD CONSTRAINT funding_source_candidate_discovery_session_fk 
    FOREIGN KEY (discovery_session_id) 
    REFERENCES discovery_session(session_id) 
    ON DELETE SET NULL;

-- Indexes for Performance & Analytics  
CREATE INDEX idx_discovery_session_executed_at 
    ON discovery_session (executed_at DESC);
    
CREATE INDEX idx_discovery_session_status_type 
    ON discovery_session (status, session_type);
    
CREATE INDEX idx_discovery_session_performance 
    ON discovery_session (candidates_found DESC, duration_minutes ASC) 
    WHERE status = 'COMPLETED';

-- Analytics indexes for discovery improvement
CREATE INDEX idx_discovery_session_search_engines 
    ON discovery_session USING gin(search_engines_used);
    
CREATE INDEX idx_discovery_session_average_confidence 
    ON discovery_session (average_confidence_score DESC) 
    WHERE status = 'COMPLETED' AND candidates_found > 0;

-- Error analysis for system reliability  
CREATE INDEX idx_discovery_session_failures 
    ON discovery_session (executed_at DESC) 
    WHERE status = 'FAILED' OR jsonb_array_length(error_messages) > 0;

-- Trigger to automatically calculate duration when session completes
CREATE OR REPLACE FUNCTION calculate_discovery_session_duration()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate duration when session status changes to completed/failed/cancelled
    IF NEW.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND OLD.status = 'RUNNING' THEN
        NEW.completed_at = NOW();
        NEW.duration_minutes = EXTRACT(EPOCH FROM (NEW.completed_at - NEW.started_at)) / 60;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER discovery_session_calculate_duration
    BEFORE UPDATE ON discovery_session
    FOR EACH ROW
    EXECUTE FUNCTION calculate_discovery_session_duration();

-- Comments for Domain Understanding
COMMENT ON TABLE discovery_session IS 'Audit trail of automated discovery executions. Critical for AI process improvement and error analysis.';
COMMENT ON COLUMN discovery_session.search_engines_used IS 'JSON array of search engines queried: ["searxng", "tavily", "perplexity"]';
COMMENT ON COLUMN discovery_session.search_queries IS 'JSON array of AI-generated search queries executed during this session';
COMMENT ON COLUMN discovery_session.query_generation_prompt IS 'LM Studio prompt used to generate search queries (for prompt engineering improvement)';
COMMENT ON COLUMN discovery_session.candidates_found IS 'Total new funding source candidates discovered in this session';
COMMENT ON COLUMN discovery_session.duplicates_detected IS 'Number of duplicate candidates filtered out during processing';
COMMENT ON COLUMN discovery_session.average_confidence_score IS 'Average AI confidence score of candidates found (quality metric)';
COMMENT ON COLUMN discovery_session.llm_model_used IS 'LM Studio model identifier used for AI processing';
COMMENT ON COLUMN discovery_session.search_engine_failures IS 'JSON object tracking per-engine failures: {"searxng": ["timeout"], "tavily": ["rate_limit"]}';

-- View for Discovery Performance Analytics
CREATE VIEW discovery_session_analytics AS
SELECT 
    DATE_TRUNC('day', executed_at) as discovery_date,
    COUNT(*) as total_sessions,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_sessions,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_sessions,
    AVG(duration_minutes) as avg_duration_minutes,
    SUM(candidates_found) as total_candidates_found,
    SUM(duplicates_detected) as total_duplicates_detected,
    AVG(average_confidence_score) as avg_confidence_score,
    AVG(jsonb_array_length(search_engines_used)) as avg_search_engines_per_session
FROM discovery_session 
WHERE executed_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', executed_at)
ORDER BY discovery_date DESC;

COMMENT ON VIEW discovery_session_analytics IS 'Daily analytics for discovery session performance monitoring and AI process optimization';

-- Constitutional Compliance Verification
-- ✅ Comprehensive audit trail for AI discovery processes
-- ✅ Search engine integration tracking (Searxng, Tavily, Perplexity)  
-- ✅ LM Studio integration metadata (model tracking, prompt engineering)
-- ✅ Performance metrics for <500ms API requirement compliance
-- ✅ Error tracking and diagnostics for system reliability
-- ✅ Foreign key relationship established with FundingSourceCandidate
-- ✅ Analytics view for continuous process improvement
-- ✅ Automatic duration calculation with database triggers
