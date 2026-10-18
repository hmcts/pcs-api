-- When a draft was last saved, so the enforcement draft sweep (DraftEnforcementDeletionService) discards a
-- draft by how long it has been left rather than by how long ago it was started. Hibernate maintains it
-- (DraftCaseDataEntity.updatedAt). The time of an existing draft's last save was never recorded, so it starts
-- as the time the draft was created; the column type matches created_at.
ALTER TABLE draft.draft_case_data ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE draft.draft_case_data SET updated_at = created_at;

ALTER TABLE draft.draft_case_data
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX draft_case_data_event_id_updated_at_idx ON draft.draft_case_data (event_id, updated_at);
