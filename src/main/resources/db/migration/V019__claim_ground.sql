CREATE TABLE claim_ground (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES claim (id),
    grounds_id VARCHAR(80) NOT NULL,
    claims_reason_text VARCHAR(500),
    other_ground_description VARCHAR(500)
);