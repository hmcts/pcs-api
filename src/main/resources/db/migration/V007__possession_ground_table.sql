CREATE TABLE possession_ground (
  id          UUID PRIMARY KEY,
  pcs_case_id UUID REFERENCES pcs_case (id),
  code        VARCHAR(50)
);
