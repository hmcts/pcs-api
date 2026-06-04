ALTER TABLE general_application
  ADD CONSTRAINT fk_general_application_party_case
    FOREIGN KEY (party_id, case_id)
      REFERENCES party (id, case_id);
