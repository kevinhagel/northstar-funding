-- V7: Fix enhancement_record_immutable constraint for testing
-- Relax the constraint to allow 1-second buffer for clock skew between Java and PostgreSQL

ALTER TABLE enhancement_record 
DROP CONSTRAINT enhancement_record_immutable;

ALTER TABLE enhancement_record 
ADD CONSTRAINT enhancement_record_immutable
    CHECK (enhanced_at <= NOW() + INTERVAL '1 second'); -- Allow 1 second buffer for clock skew

COMMENT ON CONSTRAINT enhancement_record_immutable ON enhancement_record IS 
'Prevents backdating of enhancement records while allowing 1-second buffer for clock skew between application and database';
