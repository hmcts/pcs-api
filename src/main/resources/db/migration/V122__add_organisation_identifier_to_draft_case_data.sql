ALTER TABLE draft.draft_case_data
  ADD COLUMN legal_representative_organisation_id varchar(64);

DROP INDEX if EXISTS draft_case_data_unique_idx;

CREATE UNIQUE INDEX draft_case_data_unique_by_organisation_idx
  ON draft.draft_case_data(case_reference, event_id, legal_representative_organisation_id, party_id);
