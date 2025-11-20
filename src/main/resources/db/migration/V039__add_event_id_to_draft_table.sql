-- Add event_id column to table with a default value for existing rows
ALTER TABLE draft.draft_case_data ADD COLUMN event_id VARCHAR(70);

-- Set default value for existing cases
UPDATE draft.draft_case_data SET event_id = 'resumePossessionClaim';

-- Make the column NOT NULL after existing data has been populated
ALTER TABLE draft.draft_case_data ALTER COLUMN event_id SET NOT NULL;

CREATE UNIQUE INDEX unsubmitted_case_data_case_event_unique_idx
  ON draft.draft_case_data(case_reference, event_id);

