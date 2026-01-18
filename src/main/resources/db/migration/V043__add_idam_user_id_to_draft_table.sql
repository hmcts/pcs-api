DROP INDEX IF EXISTS draft.unsubmitted_case_data_case_event_unique_idx;

ALTER TABLE draft.draft_case_data
ADD COLUMN idam_user_id UUID;

DELETE FROM draft.draft_case_data;

ALTER TABLE draft.draft_case_data
ALTER COLUMN idam_user_id SET NOT NULL;

CREATE UNIQUE INDEX draft_case_data_unique_idx
  ON draft.draft_case_data(case_reference, event_id, idam_user_id);

CREATE INDEX draft_case_data_user_id_idx
  ON draft.draft_case_data(idam_user_id);

CREATE INDEX draft_case_data_case_ref_idx
  ON draft.draft_case_data(case_reference);
