ALTER TABLE contact_preferences
  DROP COLUMN IF EXISTS preference_type,
  ADD COLUMN contact_by_email YES_NO,
  ADD COLUMN contact_by_post YES_NO;


