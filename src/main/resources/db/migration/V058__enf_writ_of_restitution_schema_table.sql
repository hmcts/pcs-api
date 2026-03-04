CREATE TABLE enf_writ_of_restitution (
    id UUID PRIMARY KEY,
    enf_case_id UUID NOT NULL REFERENCES enf_case(id) ON DELETE CASCADE,
    CONSTRAINT unique_writ_of_restitution_per_enforcement UNIQUE(enf_case_id)
);

