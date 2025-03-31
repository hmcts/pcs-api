CREATE TABLE postcode_court_mapping (
    postcode VARCHAR(20) PRIMARY KEY,
    epimid INT NOT NULL,
    legislative_country VARCHAR(80) NOT NULL,
    effective_from DATE,
    effective_to DATE,
    audit JSONB NOT NULL
);
