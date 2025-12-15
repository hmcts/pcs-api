
CREATE TABLE party_access_code (
  id             UUID PRIMARY KEY,
  case_id        UUID NOT NULL REFERENCES pcs_case(id),
  party_id       UUID NOT NULL, --Once we move to using party entities, this should be set as the FK to party(id)
  code           VARCHAR(12) NOT NULL,
  role           VARCHAR(20),
  created        TIMESTAMP,
  CONSTRAINT uq_party_access_code_case_code UNIQUE (case_id, code)
);

CREATE INDEX idx_party_access_code_case_code ON party_access_code (case_id, code);

