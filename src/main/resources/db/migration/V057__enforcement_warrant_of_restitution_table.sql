CREATE TABLE enf_warrant_of_restitution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enf_case_id UUID NOT NULL REFERENCES enf_case(id) ON DELETE CASCADE,

    CONSTRAINT unique_warrant_of_restitution_per_enforcement UNIQUE(enf_case_id)
);

CREATE INDEX idx_enf_warrant_of_restitution_case_id ON enf_warrant_of_restitution(enf_case_id);