-- Test migration for GitHub Actions DB breaking change scan.
-- Triggers the truncate-table warning only.

TRUNCATE TABLE db_breaking_change_scan_test;
