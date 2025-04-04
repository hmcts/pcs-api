CREATE TABLE postcode_court_mapping (
    postcode VARCHAR(20) PRIMARY KEY,
    epimid INT NOT NULL,
    legislative_country VARCHAR(80) NOT NULL,
    effective_from TIMESTAMP DEFAULT NULL,
    effective_to TIMESTAMP DEFAULT NULL,
    audit JSONB NOT NULL
);
