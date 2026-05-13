DROP INDEX IF EXISTS draft.draft_case_data_unique_idx;

ALTER TABLE draft.draft_case_data
  ADD COLUMN party_id UUID;

CREATE UNIQUE INDEX draft_case_data_unique_idx
  ON draft.draft_case_data(case_reference, event_id, idam_user_id, party_id);

CREATE INDEX draft_case_data_party_id_idx
  ON draft.draft_case_data(party_id);
