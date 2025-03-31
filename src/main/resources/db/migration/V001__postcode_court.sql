CREATE TABLE postcode_court_mapping (
    postcode VARCHAR(20) PRIMARY KEY,
    epimid INT NOT NULL,
    legislative_country VARCHAR(80) NOT NULL,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    audit JSONB NOT NULL
);
