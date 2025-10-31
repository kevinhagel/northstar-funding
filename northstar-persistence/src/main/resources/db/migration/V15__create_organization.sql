-- V15: Create organization table
--
-- Represents funding organizations discovered from domain homepages.
-- Organization owns a domain and hosts multiple funding programs.
--
-- Hierarchy: Organization → Domain → FundingPrograms → URLs

CREATE TABLE organization (
    organization_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identity
    name VARCHAR(500) NOT NULL,
    domain VARCHAR(255) NOT NULL REFERENCES domain(domain_name),

    -- Organization Details
    mission TEXT,
    geographic_focus TEXT,
    funding_types TEXT,

    -- Contact Information
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    office_address TEXT,

    -- Discovery Metadata
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discovery_session_id UUID REFERENCES discovery_session(session_id),
    homepage_url VARCHAR(2000),
    organization_confidence DECIMAL(3,2) CHECK (organization_confidence BETWEEN 0.00 AND 1.00),
    is_valid_funding_source BOOLEAN,

    -- Tracking
    program_count INTEGER DEFAULT 0,
    last_refreshed_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,

    -- Admin Notes
    notes TEXT,

    -- Constraints
    CONSTRAINT organization_name_domain_unique UNIQUE (name, domain)
);

-- Indexes for organization table
CREATE INDEX idx_organization_domain ON organization(domain);
CREATE INDEX idx_organization_discovered_at ON organization(discovered_at DESC);
CREATE INDEX idx_organization_is_valid ON organization(is_valid_funding_source) WHERE is_valid_funding_source = true;
CREATE INDEX idx_organization_discovery_session ON organization(discovery_session_id);
CREATE INDEX idx_organization_confidence ON organization(organization_confidence DESC);

-- Comments
COMMENT ON TABLE organization IS 'Funding organizations discovered from domain homepages (two-level judging: organization + programs)';
COMMENT ON COLUMN organization.organization_confidence IS 'Confidence score from organization-level metadata judging (0.00-1.00)';
COMMENT ON COLUMN organization.is_valid_funding_source IS 'Result of organization-level judgment';
COMMENT ON COLUMN organization.program_count IS 'Number of funding programs discovered from this organization';
