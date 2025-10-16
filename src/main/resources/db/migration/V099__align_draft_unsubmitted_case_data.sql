-- Align draft/unsubmitted case data table for local/dev stability
-- This script is intentionally defensive and idempotent to handle mixed local states

CREATE SCHEMA IF NOT EXISTS draft;

-- If an older table name exists locally, rename it to the target name
DO $$
BEGIN
  IF to_regclass('draft.draft_case_data') IS NOT NULL
     AND to_regclass('draft.unsubmitted_case_data') IS NULL THEN
    ALTER TABLE IF EXISTS draft.draft_case_data RENAME TO unsubmitted_case_data;
  END IF;
EXCEPTION WHEN others THEN
  -- ignore
END $$;

-- Ensure target table exists
CREATE TABLE IF NOT EXISTS draft.unsubmitted_case_data (
  id BIGSERIAL PRIMARY KEY,
  case_reference BIGINT UNIQUE,
  data JSONB,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- Ensure column types are correct
-- Do NOT alter id type here to avoid UUID↔BIGINT local mismatches; only enforce case_reference
ALTER TABLE draft.unsubmitted_case_data
  ALTER COLUMN case_reference TYPE BIGINT USING case_reference::BIGINT;

-- Ensure unique index exists on case_reference
DO $$
BEGIN
  IF to_regclass('draft.unsubmitted_case_data_case_reference_idx') IS NULL THEN
    CREATE UNIQUE INDEX unsubmitted_case_data_case_reference_idx
      ON draft.unsubmitted_case_data (case_reference);
  END IF;
EXCEPTION WHEN others THEN
  -- ignore
END $$;



