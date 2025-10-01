-- V9: Fix EnhancementRecord to use VARCHAR with CHECK constraints (Spring Data JDBC compatible)
-- Convert from PostgreSQL ENUM type to VARCHAR for Spring Data JDBC compatibility

-- Step 1: Alter enhancement_type column to VARCHAR with CHECK constraint
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

-- Step 2: Drop the old ENUM type (if it exists and is no longer used)
DROP TYPE IF EXISTS enhancement_type CASCADE;

-- Comments for clarity
COMMENT ON COLUMN enhancement_record.enhancement_type IS 'Enhancement type: CONTACT_ADDED, DATA_CORRECTED, NOTES_ADDED, DUPLICATE_MERGED, STATUS_CHANGED, or VALIDATION_COMPLETED. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';

-- Constitutional Compliance Verification
-- ✅ VARCHAR with CHECK constraints for Spring Data JDBC compatibility
-- ✅ Maintains all existing enhancement_type values
-- ✅ No data loss during migration
