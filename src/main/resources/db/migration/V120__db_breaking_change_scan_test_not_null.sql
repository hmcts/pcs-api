-- Test migration for GitHub Actions DB breaking change scan.
-- Triggers the not-null warning only.

ALTER TABLE db_breaking_change_scan_test
  ALTER COLUMN existing_value SET NOT NULL;
