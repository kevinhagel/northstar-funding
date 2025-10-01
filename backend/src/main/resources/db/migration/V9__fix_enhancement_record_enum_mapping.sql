-- V9: Fix EnhancementRecord to use VARCHAR with CHECK constraints (Spring Data JDBC compatible)
-- Convert from PostgreSQL ENUM type to VARCHAR for Spring Data JDBC compatibility

-- Step 1: Drop views that depend on the enhancement_type column
DROP VIEW IF EXISTS enhancement_analytics CASCADE;
DROP VIEW IF EXISTS candidate_enhancement_history CASCADE;

-- Step 2: Alter enhancement_type column to VARCHAR with CHECK constraint
ALTER TABLE enhancement_record 
    ALTER COLUMN enhancement_type TYPE VARCHAR(50);

-- Add CHECK constraint for enhancement_type
ALTER TABLE enhancement_record
    ADD CONSTRAINT enhancement_record_enhancement_type_check
    CHECK (enhancement_type IN (
        'CONTACT_ADDED',
        'DATA_CORRECTED',
        'NOTES_ADDED',
        'DUPLICATE_MERGED',
        'STATUS_CHANGED',
        'VALIDATION_COMPLETED'
    ));

-- Step 3: Drop the old ENUM type (if it exists and is no longer used)
DROP TYPE IF EXISTS enhancement_type CASCADE;

-- Step 4: Recreate the views
CREATE VIEW enhancement_analytics AS
SELECT 
    au.username,
    au.full_name,
    COUNT(*) as total_enhancements,
    AVG(er.time_spent_minutes) as avg_time_per_enhancement,
    SUM(er.time_spent_minutes) as total_time_spent,
    AVG(er.confidence_improvement) as avg_confidence_improvement,
    COUNT(*) FILTER (WHERE er.ai_assistance_used = true) as ai_assisted_enhancements,
    COUNT(DISTINCT er.candidate_id) as unique_candidates_enhanced,
    array_agg(DISTINCT er.enhancement_type) as enhancement_types_used
FROM enhancement_record er
JOIN admin_user au ON er.enhanced_by = au.user_id
WHERE er.enhanced_at >= NOW() - INTERVAL '30 days'
GROUP BY au.user_id, au.username, au.full_name
ORDER BY total_enhancements DESC;

CREATE VIEW candidate_enhancement_history AS  
SELECT 
    fsc.candidate_id,
    fsc.organization_name,
    fsc.program_name,
    fsc.status,
    COUNT(er.enhancement_id) as total_enhancements,
    SUM(er.confidence_improvement) as total_confidence_improvement,
    json_agg(
        json_build_object(
            'enhanced_at', er.enhanced_at,
            'enhanced_by', au.username,
            'type', er.enhancement_type,
            'field', er.field_name,
            'notes', er.notes
        ) ORDER BY er.enhanced_at
    ) as enhancement_history
FROM funding_source_candidate fsc
LEFT JOIN enhancement_record er ON fsc.candidate_id = er.candidate_id  
LEFT JOIN admin_user au ON er.enhanced_by = au.user_id
GROUP BY fsc.candidate_id, fsc.organization_name, fsc.program_name, fsc.status;

-- Comments for clarity
COMMENT ON COLUMN enhancement_record.enhancement_type IS 'Enhancement type: CONTACT_ADDED, DATA_CORRECTED, NOTES_ADDED, DUPLICATE_MERGED, STATUS_CHANGED, or VALIDATION_COMPLETED. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';
COMMENT ON VIEW enhancement_analytics IS 'Monthly analytics for admin user enhancement performance and quality metrics';
COMMENT ON VIEW candidate_enhancement_history IS 'Complete enhancement history for each funding source candidate with admin user attribution';

-- Constitutional Compliance Verification
-- ✅ VARCHAR with CHECK constraints for Spring Data JDBC compatibility
-- ✅ Maintains all existing enhancement_type values
-- ✅ No data loss during migration
-- ✅ Views recreated after column type change
