CREATE TABLE draft_event (
  id             UUID PRIMARY KEY,
  case_reference BIGINT       NOT NULL,
  user_id        UUID         NOT NULL,
  event_id       VARCHAR(100) NOT NULL,
  event_data     JSONB        NOT NULL
);

CREATE INDEX caseref_user_event_idx ON draft_event (case_reference, user_id, event_id);
