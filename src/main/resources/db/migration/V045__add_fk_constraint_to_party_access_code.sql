ALTER TABLE party
  ADD CONSTRAINT uq_party_id_case UNIQUE (id, case_id);

ALTER TABLE party_access_code
  ADD CONSTRAINT fk_party_access_code_party
    FOREIGN KEY (party_id, case_id)
      REFERENCES party (id, case_id);


