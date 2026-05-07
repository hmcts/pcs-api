ALTER TABLE general_application
  ADD COLUMN application_submitted_date TIMESTAMP,
  ADD COLUMN application_issued_date    TIMESTAMP;


ALTER TABLE statement_of_truth
  ALTER COLUMN completed_by DROP NOT NULL,
  ADD COLUMN completed_date TIMESTAMP;

ALTER TABLE help_with_fees
  ALTER COLUMN hwf_reference TYPE VARCHAR(60);

ALTER TABLE general_application
  ADD COLUMN client_reference VARCHAR(60);

CREATE UNIQUE INDEX general_application_case_id_client_ref
  ON general_application(case_id, client_reference);


