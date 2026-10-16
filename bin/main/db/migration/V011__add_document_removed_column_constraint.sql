UPDATE document
SET removed = FALSE
WHERE removed IS NULL;

ALTER TABLE document
ALTER COLUMN removed SET NOT NULL;