CREATE TABLE eligibility_whitelisted_epim (
    epims_id INT NOT NULL,
    eligible_from DATE NOT NULL,
    audit JSONB NOT NULL,
    PRIMARY KEY (epims_id)
);

CREATE INDEX idx_eligibility_whitelisted_epim_eligible_from ON eligibility_whitelisted_epim(eligible_from);

