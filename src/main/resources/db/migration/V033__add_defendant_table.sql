CREATE TABLE defendant
(
  id       UUID PRIMARY KEY,
  case_id  UUID REFERENCES pcs_case (id),
  forename VARCHAR(100)
);