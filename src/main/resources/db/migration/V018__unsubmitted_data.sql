CREATE SCHEMA draft;

CREATE TABLE draft.unsubmitted_case_data (
  id             UUID PRIMARY KEY,
  case_reference BIGINT NOT NULL,
  case_data      JSONB
);

CREATE INDEX unsubmitted_case_data_case_reference_idx ON draft.unsubmitted_case_data (case_reference);
