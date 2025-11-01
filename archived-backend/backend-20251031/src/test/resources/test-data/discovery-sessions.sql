-- Test data for DiscoverySession integration tests
-- Covers various session states, types, and PostgreSQL-specific features

-- Completed successful session with multiple search engines
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    NOW() - INTERVAL '3 hours',
    'scheduled-discovery',
    'SCHEDULED',
    'COMPLETED',
    25,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '2 hours 35 minutes',
    '["searxng", "tavily"]'::jsonb,
    '["EU technology funding 2024", "innovation grants Europe", "startup funding programs"]'::jsonb,
    'Find European technology and innovation funding opportunities for startups',
    42,
    6,
    150,
    0.84,
    '[]'::jsonb,
    '{}'::jsonb,
    'llama-3.1-70b',
    '{"region": "Europe", "sector": "technology", "max_results": "200"}'::jsonb
);

-- Running session (currently executing)
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'b2c3d4e5-f6g7-8901-bcde-f23456789012',
    NOW() - INTERVAL '45 minutes',
    'manual-trigger-admin',
    'MANUAL',
    'RUNNING',
    0,
    NOW() - INTERVAL '45 minutes',
    NULL,
    '["perplexity", "searxng"]'::jsonb,
    '["German startup ecosystem funding", "Berlin innovation grants"]'::jsonb,
    'Manual search for German startup funding landscape',
    18,
    2,
    65,
    0.76,
    '[]'::jsonb,
    '{}'::jsonb,
    'llama-3.1-8b',
    '{"region": "Germany", "city": "Berlin", "focus": "startups"}'::jsonb
);

-- Failed session with comprehensive error tracking
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'c3d4e5f6-g7h8-9012-cdef-345678901234',
    NOW() - INTERVAL '8 hours',
    'retry-scheduler',
    'RETRY',
    'FAILED',
    12,
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '7 hours 48 minutes',
    '["searxng", "tavily", "perplexity"]'::jsonb,
    '["research funding opportunities", "academic grants", "university partnerships"]'::jsonb,
    'Comprehensive search for academic and research funding sources',
    3,
    0,
    15,
    0.42,
    '["Connection timeout after 30 seconds", "Rate limit exceeded on multiple engines", "Invalid API response format"]'::jsonb,
    '{
        "searxng": ["Connection timeout", "Server error 503"],
        "tavily": ["Rate limit exceeded", "Authentication token expired"],
        "perplexity": ["Invalid response format", "Service temporarily unavailable"]
    }'::jsonb,
    'llama-3.1-8b',
    '{"region": "global", "sector": "research", "retry_attempt": "3"}'::jsonb
);

-- High-performance completed session
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'd4e5f6g7-h8i9-0123-defa-456789012345',
    NOW() - INTERVAL '1 day',
    'scheduled-discovery',
    'SCHEDULED',
    'COMPLETED',
    18,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '23 hours 42 minutes',
    '["searxng", "tavily", "perplexity"]'::jsonb,
    '["EU Horizon Europe funding", "EIC Accelerator program", "Digital Europe Programme", "Innovation Fund calls"]'::jsonb,
    'Comprehensive EU funding landscape scan for technology companies',
    87,
    12,
    320,
    0.91,
    '[]'::jsonb,
    '{}'::jsonb,
    'llama-3.1-70b',
    '{"region": "EU", "program": "horizon-europe", "max_results": "500", "confidence_threshold": "0.7"}'::jsonb
);

-- Cancelled session
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'e5f6g7h8-i9j0-1234-efab-567890123456',
    NOW() - INTERVAL '2 days',
    'manual-trigger-admin',
    'MANUAL',
    'CANCELLED',
    3,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days' + INTERVAL '3 minutes',
    '["perplexity"]'::jsonb,
    '["venture capital funding"]'::jsonb,
    'Quick search for VC funding opportunities',
    0,
    0,
    8,
    0.0,
    '["User cancelled session manually"]'::jsonb,
    '{}'::jsonb,
    'llama-3.1-8b',
    '{"region": "US", "sector": "venture-capital", "manual_cancel": "true"}'::jsonb
);

-- Session with partial search engine failures but still completed
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'f6g7h8i9-j0k1-2345-fabc-678901234567',
    NOW() - INTERVAL '6 hours',
    'scheduled-discovery',
    'SCHEDULED',
    'COMPLETED',
    32,
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '5 hours 28 minutes',
    '["searxng", "tavily"]'::jsonb,
    '["sustainable energy funding", "green technology grants", "climate innovation programs"]'::jsonb,
    'Search for sustainable energy and climate-focused funding opportunities',
    24,
    4,
    95,
    0.73,
    '["Searxng experienced intermittent timeouts", "Tavily had rate limiting warnings"]'::jsonb,
    '{
        "searxng": ["Intermittent timeout warnings"],
        "tavily": ["Rate limit warnings - reduced query frequency"]
    }'::jsonb,
    'llama-3.1-8b',
    '{"region": "global", "sector": "green-tech", "priority": "climate"}'::jsonb
);

-- Old completed session for trend analysis
INSERT INTO discovery_session (
    session_id, executed_at, executed_by, session_type, status,
    duration_minutes, started_at, completed_at,
    search_engines_used, search_queries, query_generation_prompt,
    candidates_found, duplicates_detected, sources_scraped, average_confidence_score,
    error_messages, search_engine_failures,
    llm_model_used, search_parameters
) VALUES (
    'g7h8i9j0-k1l2-3456-gabc-789012345678',
    NOW() - INTERVAL '7 days',
    'scheduled-discovery',
    'SCHEDULED',
    'COMPLETED',
    22,
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days' + INTERVAL '22 minutes',
    '["searxng", "perplexity"]'::jsonb,
    '["healthcare innovation funding", "medtech grants", "digital health programs"]'::jsonb,
    'Healthcare and medical technology funding landscape scan',
    35,
    5,
    128,
    0.82,
    '[]'::jsonb,
    '{}'::jsonb,
    'llama-3.1-70b',
    '{"region": "EU", "sector": "healthcare", "subsector": "digital-health"}'::jsonb
);

-- Add some comments for understanding
COMMENT ON TABLE discovery_session IS 'Test data covers various session states, JSONB operations, and PostgreSQL-specific features for comprehensive integration testing';
