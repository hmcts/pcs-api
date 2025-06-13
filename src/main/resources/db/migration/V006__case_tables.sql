CREATE TABLE pcs_case (
  id BIGSERIAL PRIMARY KEY,
  case_reference BIGINT NOT NULL,
  applicant_forename VARCHAR(255),
  CONSTRAINT pcs_case_case_reference_unique UNIQUE (case_reference)
);
