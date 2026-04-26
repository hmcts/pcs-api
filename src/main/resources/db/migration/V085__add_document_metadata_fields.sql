ALTER TABLE document
    ADD COLUMN content_type VARCHAR(200),
    ADD COLUMN size BIGINT,
    ADD COLUMN display_file_name TEXT,
    ADD COLUMN claim_id UUID REFERENCES claim(id),
    ADD COLUMN defendant_response_id UUID REFERENCES defendant_response(id),
    ADD COLUMN party_id UUID REFERENCES party(id),
    ADD COLUMN general_application_id UUID REFERENCES general_application(id);

CREATE INDEX idx_document_claim_id ON document(claim_id);
CREATE INDEX idx_document_defendant_response_id ON document(defendant_response_id);
CREATE INDEX idx_document_party_id ON document(party_id);
CREATE INDEX idx_document_general_application_id ON document(general_application_id);
