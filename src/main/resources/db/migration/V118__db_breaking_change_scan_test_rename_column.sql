-- Test migration for GitHub Actions DB breaking change scan.
-- Triggers the rename-column warning only.

ALTER TABLE db_breaking_change_scan_test
  RENAME COLUMN old_value TO new_value;
