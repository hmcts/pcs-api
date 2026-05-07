TRUNCATE claim_ground;

ALTER TABLE claim_ground
  ALTER COLUMN category SET NOT NULL,
  ALTER COLUMN is_rent_arrears SET NOT NULL;

