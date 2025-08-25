ALTER TABLE claim
  ADD COLUMN type             VARCHAR(32),
  ADD COLUMN claim_reference  VARCHAR(50),
  ADD COLUMN applicant_email  VARCHAR(100),
  ADD COLUMN respondent_email VARCHAR(100),
  ALTER COLUMN created SET DEFAULT now();

ALTER TABLE claim
  RENAME COLUMN state TO counter_claim_state;

UPDATE claim
SET type = 'MAIN_CLAIM'
WHERE type IS NULL;

ALTER TABLE claim
  ALTER COLUMN type SET NOT NULL;
--   DROP COLUMN summary

CREATE TABLE claim_event_log (
  id         UUID PRIMARY KEY,
  claim_id   UUID REFERENCES claim (id),
  created    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  invoked_by VARCHAR(50),
  event_name VARCHAR(50),
  notes      VARCHAR(255)
);


CREATE TABLE gen_app (
  id                UUID PRIMARY KEY,
  version           INTEGER,
  case_id           UUID REFERENCES public.pcs_case (id),
  created           TIMESTAMP WITH TIME ZONE,
  gen_app_reference VARCHAR(50),
  summary           VARCHAR(255),
  state             VARCHAR(40)
);

CREATE TABLE gen_app_event_log (
  id         UUID PRIMARY KEY,
  gen_app_id UUID REFERENCES gen_app (id),
  created    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  invoked_by VARCHAR(50),
  event_name VARCHAR(50),
  notes      VARCHAR(255)
);
