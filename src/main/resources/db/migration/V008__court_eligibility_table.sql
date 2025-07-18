-- HDPI-1256: Create eligibility whitelist table for EPIMS IDs
-- This table stores whitelisted EPIMS IDs that are eligible for court processes
-- with their effective dates and audit information
CREATE TABLE eligibility_whitelisted_epim (
    epims_id INT NOT NULL,          -- EPIMS court identifier
    eligible_from DATE NOT NULL,    -- Date from which the court is eligible
    audit JSONB NOT NULL,           -- Audit trail information in JSON format
    PRIMARY KEY (epims_id)
);

-- Index for efficient date range queries on eligibility dates
CREATE INDEX idx_eligibility_whitelisted_epim_eligible_from ON eligibility_whitelisted_epim(eligible_from); 