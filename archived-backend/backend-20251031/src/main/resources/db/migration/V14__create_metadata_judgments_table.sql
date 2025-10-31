-- Feature 004: AI-Powered Query Generation and Metadata Judging
-- Create metadata_judgments table for storing confidence scoring details

CREATE TABLE metadata_judgments (
    id                              BIGSERIAL PRIMARY KEY,
    domain_id                       UUID NOT NULL REFERENCES domain(domain_id),
    search_result_url               TEXT NOT NULL,
    search_result_title             TEXT NOT NULL,
    search_result_description       TEXT NULL,
    search_engine_source            VARCHAR(50) NOT NULL,

    -- Confidence scores (all DECIMAL(3,2) for range 0.00-1.00)
    confidence_score                DECIMAL(3,2) NOT NULL,
    funding_keywords_score          DECIMAL(3,2) NOT NULL,
    domain_credibility_score        DECIMAL(3,2) NOT NULL,
    geographic_relevance_score      DECIMAL(3,2) NOT NULL,
    organization_type_score         DECIMAL(3,2) NOT NULL,

    -- Extracted metadata
    extracted_org_name              VARCHAR(255) NULL,
    extracted_program_name          VARCHAR(255) NULL,
    keywords_found                  TEXT[] NULL,
    geographic_terms_found          TEXT[] NULL,

    -- Candidate linkage
    candidate_created               BOOLEAN NOT NULL DEFAULT FALSE,
    candidate_id                    UUID NULL REFERENCES funding_source_candidate(candidate_id),

    -- Session tracking
    session_id                      UUID NULL REFERENCES discovery_session(session_id),
    judging_timestamp               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_judgment_scores CHECK (
        confidence_score BETWEEN 0.00 AND 1.00 AND
        funding_keywords_score BETWEEN 0.00 AND 1.00 AND
        domain_credibility_score BETWEEN 0.00 AND 1.00 AND
        geographic_relevance_score BETWEEN 0.00 AND 1.00 AND
        organization_type_score BETWEEN 0.00 AND 1.00
    ),
    CONSTRAINT chk_search_engine_source CHECK (
        search_engine_source IN ('SEARXNG', 'TAVILY', 'PERPLEXITY')
    )
);

-- Indexes for common queries
CREATE INDEX idx_judgments_domain ON metadata_judgments(domain_id);
CREATE INDEX idx_judgments_confidence ON metadata_judgments(confidence_score DESC);
CREATE INDEX idx_judgments_session ON metadata_judgments(session_id);
CREATE INDEX idx_judgments_candidate ON metadata_judgments(candidate_id);
CREATE INDEX idx_judgments_timestamp ON metadata_judgments(judging_timestamp DESC);
CREATE INDEX idx_judgments_source ON metadata_judgments(search_engine_source);

-- Comments for documentation
COMMENT ON TABLE metadata_judgments IS 'Stores detailed confidence scoring for each search result';
COMMENT ON COLUMN metadata_judgments.confidence_score IS 'Weighted aggregate: funding(0.30) + credibility(0.25) + geographic(0.25) + org_type(0.20)';
COMMENT ON COLUMN metadata_judgments.keywords_found IS 'Funding keywords detected (grant, scholarship, etc.)';
COMMENT ON COLUMN metadata_judgments.geographic_terms_found IS 'Geographic terms detected (Bulgaria, Balkans, etc.)';
COMMENT ON COLUMN metadata_judgments.candidate_created IS 'TRUE if confidence >= 0.60 and candidate was created';

-- Add FK constraint to funding_source_candidate now that this table exists
ALTER TABLE funding_source_candidate
ADD CONSTRAINT fk_candidate_metadata_judgment
    FOREIGN KEY (metadata_judgment_id)
    REFERENCES metadata_judgments(id)
    ON DELETE SET NULL;
