CREATE TABLE claim_ground (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES claim (id),
    ground_id VARCHAR(80) NOT NULL,
    ground_reason VARCHAR(500)
);
