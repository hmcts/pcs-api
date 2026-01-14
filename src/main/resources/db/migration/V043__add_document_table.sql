CREATE TABLE document (
  id UUID PRIMARY KEY,
  case_id UUID REFERENCES pcs_case(id),
  url TEXT,
  file_name TEXT,
  binary_url TEXT,
  category_id TEXT,
  type TEXT
);

CREATE TABLE claim_document (
  claim_id UUID REFERENCES claim(id),
  document_id UUID REFERENCES document(id),
  PRIMARY KEY (claim_id, document_id)
);

CREATE INDEX idx_document_case_id ON document(case_id);
CREATE INDEX idx_claim_document_claim_id ON claim_document(claim_id);
CREATE INDEX idx_claim_document_document_id ON claim_document(document_id);
