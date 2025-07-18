CREATE TABLE document (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    file_name VARCHAR(100),
    file_path VARCHAR(100),
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL,
    FOREIGN KEY (case_id) REFERENCES pcs_case(id)
);

CREATE INDEX idx_document_case_id ON document(case_id);
