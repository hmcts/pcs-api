CREATE TABLE postcode_court_mapping (
    postcode VARCHAR(20) PRIMARY KEY,
    epimid INT NOT NULL,
    legislativecountry VARCHAR(80) NOT NULL,
    effectivefrom DATE,
    effectiveto DATE,
    audit JSONB NOT NULL
);
