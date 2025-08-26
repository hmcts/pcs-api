CREATE TABLE document (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id),
    file_name VARCHAR(100),
    file_path VARCHAR(500),
    content_type VARCHAR(100),
    category VARCHAR(16) NOT NULL,
    uploaded_on DATE NOT NULL
);

CREATE INDEX idx_document_case_id ON document(case_id)
