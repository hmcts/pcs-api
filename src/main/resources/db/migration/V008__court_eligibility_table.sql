CREATE TABLE court_eligibility (
    epims_id INT NOT NULL,
    eligible_from DATE NOT NULL,
    audit JSONB NOT NULL,
    PRIMARY KEY (epims_id)
);

CREATE INDEX idx_court_eligibility_eligible_from ON court_eligibility(eligible_from); 