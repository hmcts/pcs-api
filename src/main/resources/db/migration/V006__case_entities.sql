
CREATE TABLE pcs_case_entity (
                       id UUID PRIMARY KEY,
                       case_reference BIGINT
);

CREATE TABLE address_entity (
                       id UUID PRIMARY KEY,
                       case_id UUID UNIQUE NOT NULL REFERENCES pcs_case_entity(id),
                       address_line1 VARCHAR(100),
                       address_line2 VARCHAR(100),
                       address_line3 VARCHAR(100),
                       post_town VARCHAR(100),
                       county VARCHAR(100),
                       postcode VARCHAR(100),
                       country VARCHAR(100)
);

CREATE TABLE claimant_info (
                        id UUID PRIMARY KEY,
                        forename VARCHAR(100),
                        surname VARCHAR(100),
                        parent_case_id UUID REFERENCES pcs_case_entity(id)
);

CREATE TABLE general_application (
                        id UUID PRIMARY KEY,
                        case_reference BIGINT,
                        parent_case_id UUID REFERENCES pcs_case_entity(id),
                        ga_type VARCHAR(100),
                        adjustment TEXT,
                        additional_information TEXT,
                        status VARCHAR(100)
);

CREATE INDEX idx_address_entity_case_id ON address_entity(case_id);
CREATE INDEX idx_general_application_parent_case_id ON general_application(parent_case_id);
