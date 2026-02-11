-- Selected Defendants (Junction table for selective enforcement)
CREATE TABLE enf_selected_defendants (
    id UUID PRIMARY KEY,
    enf_case_id UUID NOT NULL REFERENCES enf_case(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES party(id),
    UNIQUE(enf_case_id, party_id)
);
CREATE INDEX idx_enf_selected_defendants_case ON enf_selected_defendants(enf_case_id);
CREATE INDEX idx_enf_selected_defendants_party ON enf_selected_defendants(party_id);
