CREATE TABLE documents (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id),
    file_name VARCHAR(100),
    file_path VARCHAR(500),
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_documents_case_id ON documents(case_id)
