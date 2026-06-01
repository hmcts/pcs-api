CREATE TYPE claim_activity_type AS ENUM (
    'DOCUMENTS_CREATED',
    'CLAIMANT_PACK_SENT',
    'DEFENDANT_PACK_SENT'
);

CREATE TYPE claim_activity_status AS ENUM (
    'SUCCESS',
    'FAILURE'
);

CREATE TABLE claim_activity_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id       UUID NOT NULL REFERENCES pcs_case(id),
    party_id      UUID REFERENCES party(id),
    activity_type claim_activity_type NOT NULL,
    status        claim_activity_status NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_claim_activity_log_case_id ON claim_activity_log (case_id);

CREATE INDEX idx_claim_activity_log_party_id ON claim_activity_log (party_id);
