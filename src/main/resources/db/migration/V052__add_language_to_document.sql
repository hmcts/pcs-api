-- The language a document is written in, where it is known for certain. Nullable with no backfill:
-- existing rows (all English access-code letters, plus uploads of unknown language) stay null.
ALTER TABLE document ADD COLUMN language VARCHAR(30);
