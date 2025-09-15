CREATE TABLE access_code (
  id             UUID PRIMARY KEY,
  case_reference BIGINT NOT NULL,
  code           VARCHAR(12),
  role           VARCHAR(20),
  created        TIMESTAMP
);

CREATE INDEX access_code_case_reference_code_idx ON access_code (case_reference, code);
