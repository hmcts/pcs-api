/* Add columns without constraints */
ALTER TABLE claim_party
  ADD COLUMN rank INT;

ALTER TABLE general_application
  ADD COLUMN rank INT;

/* Some unique rank value for any existing test data */
CREATE TEMPORARY SEQUENCE claim_party_placeholder_rank;
UPDATE claim_party SET rank = nextval('claim_party_placeholder_rank') WHERE rank IS NULL;

CREATE TEMPORARY SEQUENCE gen_app_placeholder_rank;
UPDATE general_application SET rank = nextval('gen_app_placeholder_rank') WHERE rank IS NULL;

/* Add constraints */
ALTER TABLE claim_party
  ALTER COLUMN rank SET NOT NULL,
  ADD CONSTRAINT uq_claim_role_rank UNIQUE (claim_id, role, rank);

ALTER TABLE general_application
  ALTER COLUMN rank SET NOT NULL,
  ADD CONSTRAINT uq_case_rank UNIQUE (case_id, rank);

