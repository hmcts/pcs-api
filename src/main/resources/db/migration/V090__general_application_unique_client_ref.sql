ALTER TABLE general_application
  ADD COLUMN client_reference VARCHAR(60);

CREATE UNIQUE INDEX general_application_case_id_client_ref
  ON general_application(case_id, client_reference);


