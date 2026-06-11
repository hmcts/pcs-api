-- Test migration for GitHub Actions DB breaking change scan.
-- Triggers the drop-column warning only.

ALTER TABLE db_breaking_change_scan_test
  DROP COLUMN IF EXISTS legacy_value;
