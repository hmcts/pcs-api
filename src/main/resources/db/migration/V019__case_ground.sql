CREATE TABLE case_ground (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES claim (id),
    grounds_id VARCHAR(255),
    claims_reason_text VARCHAR(255)
);
