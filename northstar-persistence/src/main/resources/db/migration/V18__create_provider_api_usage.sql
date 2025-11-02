-- Migration V18: Create provider_api_usage table for API usage tracking
--
-- Purpose: Track API usage for search providers to monitor:
-- - Daily quota consumption (rate limiting)
-- - Success/failure rates per provider
-- - Response times and performance metrics
-- - Error patterns (authentication, timeout, rate limit)
--
-- Used by: AbstractSearchProviderAdapter and adapter implementations
--
-- Date: 2025-11-01

CREATE TABLE provider_api_usage (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,           -- SearchEngineType enum name (BRAVE_SEARCH, SERPER, TAVILY, SEARXNG)
    query TEXT NOT NULL,                     -- Search query that was executed
    result_count INT NOT NULL,               -- Number of results returned (0 if error)
    success BOOLEAN NOT NULL,                -- Was the API call successful?
    error_type VARCHAR(100),                 -- Error type if unsuccessful (NULL if success)
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(), -- When the API call was executed
    response_time_ms INT NOT NULL            -- API response time in milliseconds
);

-- Index for querying by provider and date (rate limit tracking)
CREATE INDEX idx_provider_date ON provider_api_usage(provider, executed_at);

-- Index for success rate analysis
CREATE INDEX idx_success_date ON provider_api_usage(success, executed_at);

-- Index for error pattern analysis
CREATE INDEX idx_error_type ON provider_api_usage(error_type) WHERE error_type IS NOT NULL;

-- Comments
COMMENT ON TABLE provider_api_usage IS 'Tracks API usage for search providers for rate limiting, performance monitoring, and debugging';
COMMENT ON COLUMN provider_api_usage.provider IS 'SearchEngineType enum name (BRAVE_SEARCH, SERPER, TAVILY, SEARXNG)';
COMMENT ON COLUMN provider_api_usage.query IS 'Search query that was executed';
COMMENT ON COLUMN provider_api_usage.result_count IS 'Number of results returned (0 if error)';
COMMENT ON COLUMN provider_api_usage.success IS 'Was the API call successful?';
COMMENT ON COLUMN provider_api_usage.error_type IS 'Error type if unsuccessful (TIMEOUT, RATE_LIMIT, AUTHENTICATION, HTTP_ERROR, etc.)';
COMMENT ON COLUMN provider_api_usage.executed_at IS 'When the API call was executed';
COMMENT ON COLUMN provider_api_usage.response_time_ms IS 'API response time in milliseconds';
