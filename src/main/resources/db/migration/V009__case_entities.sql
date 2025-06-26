-- Create PCS table (parent entity)
CREATE TABLE pcs (
                   id UUID PRIMARY KEY,
                   ccd_case_reference BIGINT NOT NULL
);

CREATE TABLE address (
                       id UUID PRIMARY KEY,
                       case_id UUID UNIQUE NOT NULL REFERENCES pcs(id),
                       address_line1 VARCHAR(255),
                       address_line2 VARCHAR(255),
                       address_line3 VARCHAR(255),
                       post_town VARCHAR(255),
                       county VARCHAR(255),
                       postcode VARCHAR(20),
                       country VARCHAR(100)
);

CREATE TABLE gen_application (
                               id UUID PRIMARY KEY,
                               application_id TEXT,
                               parent_case_id UUID NOT NULL REFERENCES pcs(id),
                               adjustment TEXT,
                               additional_information TEXT,
                               status VARCHAR(255)

);

CREATE INDEX idx_address_case_id ON address(case_id);
CREATE INDEX idx_gen_application_parent_case_id ON gen_application(parent_case_id);
