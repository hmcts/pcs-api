DROP TABLE claim_ground;

CREATE TABLE claim_ground (
  id UUID PRIMARY KEY,
  claim_id UUID NOT NULL REFERENCES claim (id),
  category VARCHAR(60),
  code VARCHAR(60) NOT NULL,
  reason VARCHAR(500),
  description VARCHAR(500),
  is_rent_arrears BOOLEAN
);

ALTER TABLE pcs_case
  DROP COLUMN possession_grounds;
