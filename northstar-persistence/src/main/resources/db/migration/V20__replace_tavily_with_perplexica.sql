-- V20: Replace TAVILY with PERPLEXICA in search engine type
--
-- Tavily adapter was removed (2025-11-23, commit 8c1b213).
-- Perplexica (self-hosted AI search with LM Studio) replaces it.
--
-- Changes:
-- 1. Drop and recreate CHECK constraint on search_result.search_engine
-- 2. Update any existing TAVILY records to PERPLEXICA (if any)
-- 3. Update comments

-- Update any existing TAVILY records to PERPLEXICA
UPDATE search_result SET search_engine = 'PERPLEXICA' WHERE search_engine = 'TAVILY';

-- Drop old constraint
ALTER TABLE search_result DROP CONSTRAINT IF EXISTS search_result_engine_check;

-- Add new constraint with PERPLEXICA instead of TAVILY
ALTER TABLE search_result ADD CONSTRAINT search_result_engine_check CHECK (
    search_engine IN ('BRAVE', 'SEARXNG', 'SERPER', 'PERPLEXICA')
);

-- Update comments
COMMENT ON COLUMN search_result.search_engine IS 'BRAVE, SEARXNG, SERPER, or PERPLEXICA';
