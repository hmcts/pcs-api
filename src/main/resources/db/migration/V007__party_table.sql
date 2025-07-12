CREATE TABLE party (
  id       UUID PRIMARY KEY,
  case_id  UUID REFERENCES pcs_case (id),
  version  INTEGER,
  idam_id  UUID,
  forename VARCHAR(100),
  surname  VARCHAR(100),
  pcq_id   UUID,
  active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_idam_id ON party (idam_id);
