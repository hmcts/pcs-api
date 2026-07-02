ALTER TABLE document
  ADD COLUMN original_file_name TEXT;

-- Get original filename from patterns like:
--   * General Application GA1 - Defendant 3.pdf
--   * rentStatement - Claimant 1.pdf
UPDATE document
SET original_file_name =  regexp_replace(file_name, '(.+?)(?: GA\d+)*(?:(?: - Defendant)|(?: - Claimant)) \d+(\..+)', '\1\2')
WHERE original_file_name IS NULL;

ALTER TABLE document
  ALTER COLUMN original_file_name SET NOT NULL;
