-- Test data setup for ContactIntelligence repository tests
-- This script runs before each test method to ensure clean test state

-- Clean existing test data
DELETE FROM contact_intelligence WHERE organization LIKE '%Test%';
DELETE FROM enhancement_record WHERE candidate_id IN (
    SELECT candidate_id FROM funding_source_candidate WHERE organization_name LIKE '%Test%'
);
DELETE FROM funding_source_candidate WHERE organization_name LIKE '%Test%';

-- Insert some reference data if needed for cross-table tests
-- Note: Individual tests will create their own specific data as needed through the repository
