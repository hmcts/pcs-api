CREATE TABLE claim_uploaded_document_checklist (
    id UUID NOT NULL PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES claim (id),
    document_type VARCHAR(60) NOT NULL
);

CREATE INDEX idx_claim_uploaded_document_checklist_claim_id
    ON claim_uploaded_document_checklist (claim_id);
