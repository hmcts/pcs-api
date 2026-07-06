-- One access code per party per case: the DB-enforced invariant that stops a defendant getting
-- duplicate codes if access-code generation ever runs concurrently for the same party.
ALTER TABLE party_access_code
    ADD CONSTRAINT uq_party_access_code_case_party UNIQUE (case_id, party_id);
