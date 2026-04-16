ALTER TABLE document
    ADD COLUMN content_type VARCHAR(200),
    ADD COLUMN size BIGINT,
    ADD COLUMN defendant_response_id UUID REFERENCES defendant_response(id);

CREATE INDEX idx_document_defendant_response_id ON document(defendant_response_id);
