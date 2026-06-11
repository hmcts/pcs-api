-- Test migration for GitHub Actions DB breaking change scan.
-- This is intended to trigger the PR warning/annotation workflow only.

DROP TABLE IF EXISTS db_breaking_change_scan_test;
