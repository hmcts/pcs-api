CREATE TABLE document (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES pcs_case(id),
    file_name VARCHAR(100),
    file_path VARCHAR(500),
    content_type VARCHAR(100),
    uploaded_on DATE NOT NULL,
    category_id VARCHAR(100)
);

CREATE INDEX idx_document_case_id ON document(case_id)
