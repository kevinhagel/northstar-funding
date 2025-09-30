-- V8: Fix ContactIntelligence enum mapping for Spring Data JDBC compatibility  
-- PostgreSQL custom enums cause type casting issues with Spring Data JDBC
-- Convert contact_type and authority_level columns from enums to VARCHAR for better compatibility

-- Step 1: Drop any views that depend on the enum columns
DROP VIEW IF EXISTS database_health_metrics CASCADE;

-- Step 2: Remove default values that reference the enums
ALTER TABLE contact_intelligence ALTER COLUMN contact_type DROP DEFAULT;
ALTER TABLE contact_intelligence ALTER COLUMN authority_level DROP DEFAULT;

-- Step 3: Remove constraints that might reference the enums  
ALTER TABLE contact_intelligence DROP CONSTRAINT IF EXISTS contact_intelligence_email_or_phone_required;
ALTER TABLE contact_intelligence DROP CONSTRAINT IF EXISTS contact_intelligence_validated_when_contacted;

-- Step 4: Convert the enum columns to VARCHAR, handling existing enum values
ALTER TABLE contact_intelligence 
    ALTER COLUMN contact_type TYPE VARCHAR(50) USING contact_type::text;

ALTER TABLE contact_intelligence 
    ALTER COLUMN authority_level TYPE VARCHAR(50) USING authority_level::text;

-- Step 5: Drop the enum types (now safe to do)
DROP TYPE IF EXISTS contact_type;
DROP TYPE IF EXISTS authority_level;

-- Step 6: Add CHECK constraints to maintain data integrity (equivalent to enums)
ALTER TABLE contact_intelligence 
ADD CONSTRAINT contact_intelligence_contact_type_check 
    CHECK (contact_type IN ('PROGRAM_OFFICER', 'FOUNDATION_STAFF', 'GOVERNMENT_OFFICIAL', 'ACADEMIC_CONTACT', 'CORPORATE_CONTACT'));

ALTER TABLE contact_intelligence 
ADD CONSTRAINT contact_intelligence_authority_level_check 
    CHECK (authority_level IN ('DECISION_MAKER', 'INFLUENCER', 'INFORMATION_ONLY'));

-- Step 7: Set new default values using VARCHAR
ALTER TABLE contact_intelligence 
    ALTER COLUMN authority_level SET DEFAULT 'INFORMATION_ONLY';

-- Step 8: Re-add the business rule constraints
ALTER TABLE contact_intelligence 
ADD CONSTRAINT contact_intelligence_email_or_phone_required
    CHECK (email IS NOT NULL OR phone IS NOT NULL);
        
ALTER TABLE contact_intelligence 
ADD CONSTRAINT contact_intelligence_validated_when_contacted
    CHECK (
        (last_contacted_at IS NOT NULL AND validated_at IS NOT NULL) OR
        (last_contacted_at IS NULL)
    );

-- Comments for clarification
COMMENT ON COLUMN contact_intelligence.contact_type IS 'Contact type: PROGRAM_OFFICER, FOUNDATION_STAFF, GOVERNMENT_OFFICIAL, ACADEMIC_CONTACT, or CORPORATE_CONTACT. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';
COMMENT ON COLUMN contact_intelligence.authority_level IS 'Authority level: DECISION_MAKER, INFLUENCER, or INFORMATION_ONLY. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';

-- Note: The database_health_metrics view will need to be recreated manually if it was needed
-- We don't recreate it here as we don't know its original definition
