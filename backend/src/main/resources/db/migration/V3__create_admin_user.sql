-- V3: Create AdminUser Table (System Administrators)
-- Human-AI Collaboration: Admin users who review and validate AI-discovered candidates
-- Review Statistics: Performance tracking for workload balancing
-- Constitutional: Support for Kevin & Huw as primary admin users

-- Drop table if exists (for development)  
DROP TABLE IF EXISTS admin_user CASCADE;

-- Create ENUM types for Admin User domain
CREATE TYPE admin_role AS ENUM (
    'ADMINISTRATOR',
    'REVIEWER'
);

-- AdminUser: System administrators for human validation workflows
CREATE TABLE admin_user (
    -- Primary Key & Identity
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(300) NOT NULL UNIQUE,
    
    -- Role & Authorization
    role admin_role NOT NULL DEFAULT 'REVIEWER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ NULL,
    
    -- Review Performance Statistics (Workload Balancing)
    candidates_reviewed INTEGER NOT NULL DEFAULT 0,
    average_review_time_minutes INTEGER DEFAULT 0,
    approval_rate DECIMAL(4,2) DEFAULT 0.00 CHECK (approval_rate >= 0.00 AND approval_rate <= 1.00),
    
    -- Specialization & Assignment Logic
    specializations JSONB DEFAULT '[]'::jsonb, -- Areas of expertise for smart assignment
    current_workload INTEGER NOT NULL DEFAULT 0, -- Active assignments for load balancing
    max_concurrent_assignments INTEGER DEFAULT 10,
    
    -- Constitutional Requirement: Support for Primary Users
    CONSTRAINT admin_user_kevin_huw_founders 
        CHECK (
            username IN ('kevin', 'huw') OR 
            role = 'REVIEWER'
        ),
        
    CONSTRAINT admin_user_workload_limits
        CHECK (current_workload <= max_concurrent_assignments)
);

-- Now we can add the foreign key constraints that were deferred from V1 and V2
-- Add foreign key to FundingSourceCandidate.assigned_reviewer_id
ALTER TABLE funding_source_candidate 
ADD CONSTRAINT funding_source_candidate_assigned_reviewer_fk 
    FOREIGN KEY (assigned_reviewer_id) 
    REFERENCES admin_user(user_id) 
    ON DELETE SET NULL;

-- Add foreign key to ContactIntelligence.created_by  
ALTER TABLE contact_intelligence 
ADD CONSTRAINT contact_intelligence_created_by_fk 
    FOREIGN KEY (created_by) 
    REFERENCES admin_user(user_id) 
    ON DELETE SET NULL;

-- Indexes for Performance & Assignment Queries
CREATE INDEX idx_admin_user_role_active 
    ON admin_user (role) 
    WHERE is_active = true;
    
CREATE INDEX idx_admin_user_workload 
    ON admin_user (current_workload, role) 
    WHERE is_active = true;
    
CREATE INDEX idx_admin_user_specializations 
    ON admin_user USING gin(specializations) 
    WHERE is_active = true;
    
CREATE INDEX idx_admin_user_last_login 
    ON admin_user (last_login_at DESC) 
    WHERE is_active = true;

-- Performance statistics for review analysis
CREATE INDEX idx_admin_user_approval_rate 
    ON admin_user (approval_rate DESC) 
    WHERE is_active = true AND candidates_reviewed > 0;

-- Insert Founder Admin Users (Constitutional Requirement)
INSERT INTO admin_user (
    username, 
    full_name, 
    email, 
    role, 
    max_concurrent_assignments,
    specializations
) VALUES 
(
    'kevin', 
    'Kevin Hagel', 
    'kevin@northstar.bg', 
    'ADMINISTRATOR',
    20,
    '["technology", "academic", "research", "government"]'::jsonb
),
(
    'huw', 
    'Huw Jones', 
    'huw@northstar.bg', 
    'ADMINISTRATOR', 
    20,
    '["foundation", "corporate", "international", "arts"]'::jsonb
);

-- Comments for Domain Understanding
COMMENT ON TABLE admin_user IS 'System administrators for human-AI collaboration workflows. Kevin & Huw as founders with ADMINISTRATOR role.';
COMMENT ON COLUMN admin_user.candidates_reviewed IS 'Total funding source candidates processed by this admin user';
COMMENT ON COLUMN admin_user.average_review_time_minutes IS 'Average time spent reviewing each candidate (for workload estimation)';
COMMENT ON COLUMN admin_user.approval_rate IS 'Percentage of candidates approved vs rejected (quality metric)';
COMMENT ON COLUMN admin_user.specializations IS 'Areas of expertise as JSON array for intelligent assignment of candidates';
COMMENT ON COLUMN admin_user.current_workload IS 'Number of candidates currently assigned to this user (real-time counter)';
COMMENT ON COLUMN admin_user.max_concurrent_assignments IS 'Maximum number of candidates that can be assigned simultaneously';

-- Trigger to update last_login_at (will be called from application)
CREATE OR REPLACE FUNCTION update_admin_user_last_login()
RETURNS TRIGGER AS $$
BEGIN
    -- This will be triggered from application layer during authentication
    NEW.last_login_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Constitutional Compliance Verification
-- ✅ Kevin & Huw as founder ADMINISTRATOR users inserted
-- ✅ Role-based access control with ADMINISTRATOR/REVIEWER distinction  
-- ✅ Review performance statistics for workload balancing
-- ✅ Specialization-based assignment support (JSON array for flexibility)
-- ✅ Foreign key relationships established with previously created tables
-- ✅ Current workload tracking for real-time assignment decisions
-- ✅ Human-AI collaboration support through reviewer assignment logic
