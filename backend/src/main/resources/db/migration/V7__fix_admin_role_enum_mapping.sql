-- V7: Fix AdminRole enum mapping for Spring Data JDBC compatibility
-- PostgreSQL custom enums cause type casting issues with Spring Data JDBC
-- Convert role column from admin_role enum to VARCHAR for better compatibility

-- Step 1: Remove the default value that references the enum
ALTER TABLE admin_user ALTER COLUMN role DROP DEFAULT;

-- Step 2: Remove any constraints that might reference the enum
ALTER TABLE admin_user DROP CONSTRAINT IF EXISTS admin_user_kevin_huw_founders;

-- Step 3: Convert the role column to VARCHAR, handling existing enum values
ALTER TABLE admin_user 
    ALTER COLUMN role TYPE VARCHAR(50) USING role::text;

-- Step 4: Drop the enum type (now safe to do)
DROP TYPE IF EXISTS admin_role;

-- Step 5: Add CHECK constraint to maintain data integrity (equivalent to enum)
ALTER TABLE admin_user 
ADD CONSTRAINT admin_user_role_check 
    CHECK (role IN ('ADMINISTRATOR', 'REVIEWER'));

-- Step 6: Set new default value using VARCHAR
ALTER TABLE admin_user 
    ALTER COLUMN role SET DEFAULT 'REVIEWER';

-- Step 7: Re-add the founders constraint with VARCHAR comparison
ALTER TABLE admin_user 
ADD CONSTRAINT admin_user_kevin_huw_founders 
    CHECK (
        username IN ('kevin', 'huw') OR 
        role = 'REVIEWER'
    );

-- Comments for clarification
COMMENT ON COLUMN admin_user.role IS 'Admin role: ADMINISTRATOR or REVIEWER. Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';
