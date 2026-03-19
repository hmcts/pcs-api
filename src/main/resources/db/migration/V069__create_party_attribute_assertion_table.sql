CREATE TYPE party_attribute_assertion_submitted_by AS ENUM (
    'CLAIMANT',
    'DEFENDANT',
    'JUDGE',
    'COURT_STAFF',
    'CASE_WORKER'
);

CREATE TYPE party_attribute_assertion_status AS ENUM (
    'SUBMITTED',
    'UNDER_REVIEW',
    'ACCEPTED',
    'REJECTED'
);

CREATE TABLE party_attribute_assertion (
     id UUID PRIMARY KEY,
     party_id UUID NOT NULL REFERENCES party(id),
     evidence_document_id UUID NOT NULL REFERENCES document(id),
     attributes_name VARCHAR(255) NOT NULL,
     asserted_value TEXT NOT NULL,
     asserted_by party_attribute_assertion_submitted_by NOT NULL,
     status party_attribute_assertion_status NOT NULL,
     created_at TIMESTAMP NOT NULL,
     decided_at TIMESTAMP
);

CREATE INDEX idx_party_attribute_assertion_party_id ON party_attribute_assertion (party_id);

