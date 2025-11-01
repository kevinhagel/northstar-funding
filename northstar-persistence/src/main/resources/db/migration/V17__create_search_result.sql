-- V17: Create search_result table
--
-- Tracks raw search results from search engines for deduplication.
-- Critical for preventing reprocessing of same URLs on same day.
--
-- Deduplication Logic:
-- - Same domain + same URL + same day = Skip (duplicate)
-- - Same domain + different URL = Process (different program)
-- - New domain = Process (new organization)
--
-- Search Engines: BRAVE, SEARXNG, SERPER, TAVILY

CREATE TABLE search_result (
    search_result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Search Metadata
    discovery_session_id UUID NOT NULL REFERENCES discovery_session(session_id),
    search_engine VARCHAR(50) NOT NULL,
    search_query TEXT NOT NULL,
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    search_date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- URL & Domain Information
    url VARCHAR(2000) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    url_path VARCHAR(2000),

    -- Search Engine Metadata (Phase 1 - Metadata Judging)
    title VARCHAR(500),
    description TEXT,
    rank_position INTEGER,

    -- Deduplication Tracking
    is_duplicate BOOLEAN DEFAULT false,
    duplicate_of_result_id UUID REFERENCES search_result(search_result_id),
    deduplication_key VARCHAR(500) NOT NULL,

    -- Processing Status
    is_processed BOOLEAN DEFAULT false,
    processed_at TIMESTAMP,
    organization_id UUID REFERENCES organization(organization_id),
    program_id UUID REFERENCES funding_program(program_id),
    candidate_id UUID REFERENCES funding_source_candidate(candidate_id),

    -- Quality Tracking
    is_blacklisted BOOLEAN DEFAULT false,
    blacklist_reason TEXT,
    notes TEXT,

    -- Constraints
    CONSTRAINT search_result_engine_check CHECK (
        search_engine IN ('BRAVE', 'SEARXNG', 'SERPER', 'TAVILY')
    ),
    CONSTRAINT search_result_dedup_unique UNIQUE (deduplication_key)
);

-- Indexes for search_result table
CREATE INDEX idx_search_result_discovery_session ON search_result(discovery_session_id);
CREATE INDEX idx_search_result_domain ON search_result(domain);
CREATE INDEX idx_search_result_url ON search_result(url);
CREATE INDEX idx_search_result_search_date ON search_result(search_date DESC);
CREATE INDEX idx_search_result_dedup_key ON search_result(deduplication_key);
CREATE INDEX idx_search_result_is_duplicate ON search_result(is_duplicate) WHERE is_duplicate = false;
CREATE INDEX idx_search_result_is_processed ON search_result(is_processed) WHERE is_processed = false;
CREATE INDEX idx_search_result_is_blacklisted ON search_result(is_blacklisted) WHERE is_blacklisted = false;
CREATE INDEX idx_search_result_engine ON search_result(search_engine);

-- Add foreign key to funding_program.search_result_id now that search_result exists
ALTER TABLE funding_program ADD CONSTRAINT fk_funding_program_search_result
    FOREIGN KEY (search_result_id) REFERENCES search_result(search_result_id);

-- Comments
COMMENT ON TABLE search_result IS 'Raw search results for deduplication tracking (Phase 1 metadata judging)';
COMMENT ON COLUMN search_result.deduplication_key IS 'Format: domain:url:YYYY-MM-DD for deduplication';
COMMENT ON COLUMN search_result.is_duplicate IS 'True if same domain + same URL + same day';
COMMENT ON COLUMN search_result.search_engine IS 'BRAVE, SEARXNG, SERPER, or TAVILY';
COMMENT ON COLUMN search_result.is_processed IS 'Has this result been processed into Organization/Program/Candidate?';
COMMENT ON COLUMN search_result.is_blacklisted IS 'Was this domain blacklisted?';
