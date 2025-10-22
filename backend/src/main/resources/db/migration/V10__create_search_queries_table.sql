-- V10__create_search_queries_table.sql
-- Feature: 003-search-execution-infrastructure
-- Purpose: Create search_queries table for hardcoded query library
-- Constitutional Compliance: PostgreSQL 16, TEXT[] for all array columns

-- Create search_queries table
CREATE TABLE search_queries (
    id                      BIGSERIAL PRIMARY KEY,
    query_text              VARCHAR(500) NOT NULL,
    day_of_week             VARCHAR(20) NOT NULL,
    tags                    TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    target_engines          TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    expected_results        INTEGER NOT NULL DEFAULT 25,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),

    -- Feature 004: AI Generation Fields
    generation_method       VARCHAR(20) NOT NULL DEFAULT 'HARDCODED',
    ai_model_used           VARCHAR(100) NULL,
    query_template_id       VARCHAR(50) NULL,
    semantic_cluster_id     INTEGER NULL,
    generation_session_id   BIGINT NULL, -- FK added after query_generation_sessions exists
    generation_date         DATE NULL,

    -- Constraints
    CONSTRAINT chk_search_queries_query_text_not_empty CHECK (LENGTH(TRIM(query_text)) > 0),
    CONSTRAINT chk_search_queries_expected_results CHECK (expected_results BETWEEN 1 AND 100),
    CONSTRAINT chk_search_queries_day_of_week CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),

    -- Feature 004: AI Generation Constraints
    CONSTRAINT chk_generation_method
        CHECK (generation_method IN ('AI_GENERATED', 'HARDCODED')),

    CONSTRAINT chk_ai_generated_fields
        CHECK (
            (generation_method = 'HARDCODED') OR
            (generation_method = 'AI_GENERATED' AND generation_date IS NOT NULL AND ai_model_used IS NOT NULL)
        )
);

-- Create indexes for common queries
CREATE INDEX idx_search_queries_day_of_week ON search_queries(day_of_week) WHERE enabled = TRUE;
CREATE INDEX idx_search_queries_enabled ON search_queries(enabled);
CREATE INDEX idx_search_queries_tags ON search_queries USING GIN(tags);
CREATE INDEX idx_search_queries_target_engines ON search_queries USING GIN(target_engines);

-- Feature 004: AI Generation Indexes
CREATE INDEX idx_queries_generation_method ON search_queries(generation_method);
CREATE INDEX idx_queries_generation_date ON search_queries(generation_date DESC);
CREATE INDEX idx_queries_session ON search_queries(generation_session_id);

-- Add comment for documentation
COMMENT ON TABLE search_queries IS 'Hardcoded query library for nightly search execution. Queries are organized by day of week and tagged with geography/category/authority metadata.';
COMMENT ON COLUMN search_queries.query_text IS 'Search query text (e.g., "bulgaria education grants 2025")';
COMMENT ON COLUMN search_queries.day_of_week IS 'Day of week when this query should be executed (enum: MONDAY-SUNDAY)';
COMMENT ON COLUMN search_queries.tags IS 'TEXT[] array of tag strings in format "TYPE:value": {"GEOGRAPHY:Bulgaria", "CATEGORY:Education"}';
COMMENT ON COLUMN search_queries.target_engines IS 'TEXT[] array of SearchEngineType enum values: {SEARXNG, TAVILY, PERPLEXITY}';
COMMENT ON COLUMN search_queries.expected_results IS 'Expected number of results (1-100, default 25)';
COMMENT ON COLUMN search_queries.enabled IS 'Whether this query is currently active in the nightly execution';
