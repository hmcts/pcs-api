CREATE TYPE CONTACT_PREFERENCE_TYPE AS ENUM ('EMAIL', 'POST');

ALTER TABLE contact_preferences
    ADD COLUMN preference_type CONTACT_PREFERENCE_TYPE,
    DROP COLUMN contact_by_email,
    DROP COLUMN contact_by_post;
