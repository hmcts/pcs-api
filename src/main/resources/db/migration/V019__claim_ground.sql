CREATE TABLE claim_ground (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES claim (id),
    grounds_id VARCHAR(255),
    claims_reason_text VARCHAR(255)
);
