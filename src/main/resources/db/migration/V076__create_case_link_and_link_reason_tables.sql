CREATE TABLE case_link (
     id UUID PRIMARY KEY,
     case_id UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
     linked_case_reference BIGINT NOT NULL,
     ccd_list_id VARCHAR(50),
     created_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX ux_case_link_unique
  ON case_link(case_id, linked_case_reference);

CREATE TABLE case_link_reason (
    id UUID PRIMARY KEY,
    case_link_id UUID NOT NULL REFERENCES case_link(id) ON DELETE CASCADE,
    reason_code VARCHAR(100) NOT NULL
);

CREATE INDEX idx_case_link_reason_link
  ON case_link_reason(case_link_id);
