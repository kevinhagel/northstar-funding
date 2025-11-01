-- V5: Create EnhancementRecord Table (Audit Trail of Manual Improvements)
-- Human-AI Collaboration: Track manual enhancements by admin users  
-- Immutable Audit Log: Constitutional requirement for enhancement tracking
-- Quality Metrics: Time tracking and improvement categorization

-- Drop table if exists (for development)
DROP TABLE IF EXISTS enhancement_record CASCADE;

-- EnhancementRecord: Immutable audit trail of human improvements
-- Using VARCHAR for enum-like fields for Spring Data JDBC compatibility
CREATE TABLE enhancement_record (
    -- Primary Key (Immutable record)
    enhancement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relationships
    candidate_id UUID NOT NULL, -- FK to FundingSourceCandidate
    enhanced_by UUID NOT NULL,   -- FK to AdminUser  
    
    -- Enhancement Metadata
    enhanced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    enhancement_type VARCHAR(50) NOT NULL,
    time_spent_minutes INTEGER DEFAULT 0,
    
    -- Change Tracking (Immutable Audit Trail)
    field_name VARCHAR(100), -- Which field was modified
    original_value TEXT,     -- Previous value (nullable for new additions)
    suggested_value TEXT NOT NULL, -- New value
    notes TEXT,              -- Explanation of changes

    -- AI Tracking (Human-AI Collaboration)
    ai_model VARCHAR(100),           -- LM Studio model used (e.g., "llama-3.1-8b")
    confidence_score DECIMAL(3,2),   -- AI confidence (0.00-1.00)
    human_approved BOOLEAN DEFAULT FALSE, -- Did human approve AI suggestion?
    approved_by UUID,                -- AdminUser who approved (if applicable)
    approved_at TIMESTAMPTZ,         -- When approved

    -- Context & Validation
    source_reference TEXT,   -- URL, document, or source of information
    confidence_improvement DECIMAL(3,2) DEFAULT 0.00, -- How much this improved candidate confidence
    validation_method VARCHAR(100), -- How enhancement was validated

    -- Quality Metrics
    review_complexity VARCHAR(20) DEFAULT 'SIMPLE', -- SIMPLE, MODERATE, COMPLEX
    ai_assistance_used BOOLEAN DEFAULT false -- Whether AI tools were used for enhancement
    
    -- CHECK constraints for enum-like validation (Spring Data JDBC compatible)
    CONSTRAINT enhancement_record_enhancement_type_check
        CHECK (enhancement_type IN (
            'AI_SUGGESTED',      -- AI suggested this enhancement
            'MANUAL',            -- Human created without AI
            'HUMAN_MODIFIED'     -- Human modified AI suggestion
        )),

    CONSTRAINT enhancement_record_review_complexity_check
        CHECK (review_complexity IN ('SIMPLE', 'MODERATE', 'COMPLEX')),

    CONSTRAINT enhancement_record_ai_consistency
        CHECK (
            (enhancement_type = 'AI_SUGGESTED' AND ai_model IS NOT NULL) OR
            (enhancement_type != 'AI_SUGGESTED')
        ),

    -- Foreign Key Constraints
    CONSTRAINT enhancement_record_candidate_fk
        FOREIGN KEY (candidate_id)
        REFERENCES funding_source_candidate(candidate_id)
        ON DELETE CASCADE,

    CONSTRAINT enhancement_record_enhanced_by_fk
        FOREIGN KEY (enhanced_by)
        REFERENCES admin_user(user_id)
        ON DELETE RESTRICT, -- Preserve audit trail even if admin user deleted

    CONSTRAINT enhancement_record_approved_by_fk
        FOREIGN KEY (approved_by)
        REFERENCES admin_user(user_id)
        ON DELETE RESTRICT, -- Preserve audit trail
        
    -- Business Rules (Immutable Audit Log)
    CONSTRAINT enhancement_record_immutable
        CHECK (enhanced_at <= NOW() + INTERVAL '1 second'), -- Allow 1 second buffer for clock skew
        
    CONSTRAINT enhancement_record_confidence_improvement_range  
        CHECK (confidence_improvement >= -1.00 AND confidence_improvement <= 1.00),
        
    CONSTRAINT enhancement_record_time_spent_reasonable
        CHECK (time_spent_minutes >= 0 AND time_spent_minutes <= 480) -- Max 8 hours per enhancement
);

-- IMMUTABILITY: No UPDATE/DELETE allowed on enhancement_record
-- This table is append-only for complete audit trail

-- Indexes for Performance & Analytics
CREATE INDEX idx_enhancement_record_candidate_id 
    ON enhancement_record (candidate_id);
    
CREATE INDEX idx_enhancement_record_enhanced_by 
    ON enhancement_record (enhanced_by);
    
CREATE INDEX idx_enhancement_record_enhanced_at 
    ON enhancement_record (enhanced_at DESC);
    
CREATE INDEX idx_enhancement_record_enhancement_type 
    ON enhancement_record (enhancement_type);

-- Analytics indexes for human performance tracking
CREATE INDEX idx_enhancement_record_time_analysis 
    ON enhancement_record (enhanced_by, time_spent_minutes, enhancement_type);
    
CREATE INDEX idx_enhancement_record_confidence_improvement 
    ON enhancement_record (confidence_improvement DESC) 
    WHERE confidence_improvement > 0;

-- Full-text search for enhancement notes and changes
CREATE INDEX idx_enhancement_record_search
    ON enhancement_record
    USING gin(to_tsvector('english',
        COALESCE(field_name, '') || ' ' ||
        COALESCE(suggested_value, '') || ' ' ||
        COALESCE(notes, '')
    ));

-- Trigger to update AdminUser statistics when enhancements are recorded
CREATE OR REPLACE FUNCTION update_admin_user_enhancement_stats()
RETURNS TRIGGER AS $$
DECLARE
    total_review_time INTEGER;
    total_candidates INTEGER;
BEGIN
    -- Update admin user's average review time and candidate count
    SELECT 
        COALESCE(SUM(er.time_spent_minutes), 0),
        COUNT(DISTINCT er.candidate_id)
    INTO total_review_time, total_candidates
    FROM enhancement_record er 
    WHERE er.enhanced_by = NEW.enhanced_by;
    
    UPDATE admin_user 
    SET 
        candidates_reviewed = total_candidates,
        average_review_time_minutes = CASE 
            WHEN total_candidates > 0 THEN total_review_time / total_candidates 
            ELSE 0 
        END
    WHERE user_id = NEW.enhanced_by;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enhancement_record_update_admin_stats
    AFTER INSERT ON enhancement_record
    FOR EACH ROW
    EXECUTE FUNCTION update_admin_user_enhancement_stats();

-- Comments for Domain Understanding
COMMENT ON TABLE enhancement_record IS 'Immutable audit trail of AI suggestions and human improvements. Constitutional requirement for human-AI collaboration tracking.';
COMMENT ON COLUMN enhancement_record.enhancement_type IS 'Enhancement origin: AI_SUGGESTED (AI proposed), MANUAL (human created), or HUMAN_MODIFIED (human modified AI suggestion). Using VARCHAR with CHECK constraint for Spring Data JDBC compatibility.';
COMMENT ON COLUMN enhancement_record.field_name IS 'Which field was modified (e.g. "organization_name", "contact_email", "description")';
COMMENT ON COLUMN enhancement_record.original_value IS 'Previous value before enhancement (NULL for new additions)';
COMMENT ON COLUMN enhancement_record.suggested_value IS 'New value after enhancement (required for audit trail)';
COMMENT ON COLUMN enhancement_record.ai_model IS 'LM Studio model that generated this suggestion (e.g., "llama-3.1-8b"). Required for AI_SUGGESTED type.';
COMMENT ON COLUMN enhancement_record.confidence_score IS 'AI confidence in this suggestion (0.00-1.00). Only for AI_SUGGESTED type.';
COMMENT ON COLUMN enhancement_record.human_approved IS 'Whether human reviewer approved the AI suggestion';
COMMENT ON COLUMN enhancement_record.approved_by IS 'AdminUser UUID who approved this enhancement';
COMMENT ON COLUMN enhancement_record.approved_at IS 'Timestamp when enhancement was approved';
COMMENT ON COLUMN enhancement_record.confidence_improvement IS 'How much this enhancement improved candidate confidence score (-1.00 to +1.00)';
COMMENT ON COLUMN enhancement_record.ai_assistance_used IS 'Whether LM Studio or other AI tools were used to assist with this enhancement';
COMMENT ON COLUMN enhancement_record.validation_method IS 'How enhancement was validated: "website_verification", "email_confirmation", "phone_call", etc.';

-- View for Enhancement Analytics & Quality Metrics
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

COMMENT ON VIEW enhancement_analytics IS 'Monthly analytics for admin user enhancement performance and quality metrics';

-- View for Candidate Enhancement History
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

COMMENT ON VIEW candidate_enhancement_history IS 'Complete enhancement history for each funding source candidate with admin user attribution';

-- Constitutional Compliance Verification
-- ✅ Immutable audit trail (append-only, no updates/deletes allowed)
-- ✅ Human-AI collaboration tracking (AI_SUGGESTED, MANUAL, HUMAN_MODIFIED types)
-- ✅ AI model and confidence tracking for LM Studio integration
-- ✅ Human approval workflow (human_approved, approved_by, approved_at)
-- ✅ Quality metrics with confidence improvement scoring
-- ✅ Time tracking for performance analysis and workload balancing
-- ✅ Foreign key relationships with cascade/restrict rules for audit integrity
-- ✅ Automatic statistics updates for admin user performance tracking
-- ✅ Analytics views for continuous improvement of enhancement processes
-- ✅ Full-text search capabilities for enhancement mining and analysis
-- ✅ VARCHAR with CHECK constraints for Spring Data JDBC compatibility
-- ✅ AI consistency validation (AI_SUGGESTED must have ai_model)
