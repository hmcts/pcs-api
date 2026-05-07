ALTER TABLE document
    ADD COLUMN submitted_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

UPDATE document
SET submitted_date = COALESCE(submitted_date, CURRENT_TIMESTAMP);

ALTER TABLE document
    ALTER COLUMN submitted_date SET NOT NULL;
