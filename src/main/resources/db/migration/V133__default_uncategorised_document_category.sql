UPDATE document
SET category_id = 'uncategorisedDocuments'
WHERE category_id IS NULL;

ALTER TABLE document
ALTER COLUMN category_id SET DEFAULT 'uncategorisedDocuments';

ALTER TABLE document
ALTER COLUMN category_id SET NOT NULL;
