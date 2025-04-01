CREATE TABLE postcode_court_mapping (
    postcode VARCHAR(20) NOT NULL,
    epimid INT NOT NULL,
    legislative_country VARCHAR(80) NOT NULL,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    audit JSONB NOT NULL,
    PRIMARY KEY (postcode, epimid)
);

CREATE INDEX idx_postcode ON postcode_court_mapping(postcode);
