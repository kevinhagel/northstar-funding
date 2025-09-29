-- Discovery Session Test Data
-- This file provides test data for DiscoverySessionRepositoryTest

-- Clear existing data
DELETE FROM discovery_sessions;

-- Insert test discovery sessions
INSERT INTO discovery_sessions (
    session_id, 
    session_type, 
    status, 
    created_by_user_id, 
    started_at, 
    completed_at, 
    last_updated_at,
    search_engines_used,
    total_queries_executed,
    total_results_discovered,
    total_candidates_created,
    error_count,
    execution_notes
) VALUES 
(
    '11111111-1111-1111-1111-111111111111',
    'AUTOMATED',
    'COMPLETED', 
    '22222222-2222-2222-2222-222222222222',
    '2024-01-15T10:00:00',
    '2024-01-15T10:30:00',
    '2024-01-15T10:30:00',
    ARRAY['searxng', 'bing'],
    25,
    150,
    12,
    0,
    'Successful automated discovery session'
),
(
    '33333333-3333-3333-3333-333333333333',
    'MANUAL',
    'IN_PROGRESS', 
    '44444444-4444-4444-4444-444444444444',
    '2024-01-16T14:00:00',
    NULL,
    '2024-01-16T14:15:00',
    ARRAY['searxng'],
    8,
    45,
    3,
    1,
    'Manual discovery session in progress'
),
(
    '55555555-5555-5555-5555-555555555555',
    'AUTOMATED',
    'FAILED', 
    '22222222-2222-2222-2222-222222222222',
    '2024-01-17T08:00:00',
    '2024-01-17T08:05:00',
    '2024-01-17T08:05:00',
    ARRAY['searxng'],
    2,
    0,
    0,
    5,
    'Search engine connection failed'
);
