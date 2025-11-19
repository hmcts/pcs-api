-- Add event_id column to table.
ALTER TABLE draft.draft_case_data ADD COLUMN event_id VARCHAR(70) NOT NULL;

CREATE UNIQUE INDEX unsubmitted_case_data_case_event_unique_idx
  ON draft.draft_case_data(case_reference, event_id);
