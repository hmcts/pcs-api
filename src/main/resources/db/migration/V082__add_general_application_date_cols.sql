ALTER TABLE general_application
  ADD COLUMN application_submitted_date TIMESTAMP,
  ADD COLUMN application_issued_date    TIMESTAMP;


ALTER TABLE statement_of_truth
  ALTER COLUMN completed_by DROP NOT NULL,
  ADD COLUMN completed_date TIMESTAMP;
