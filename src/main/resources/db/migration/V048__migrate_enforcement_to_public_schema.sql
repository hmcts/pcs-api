ALTER TABLE enforcement.enf_case SET SCHEMA public;

DO $$
  BEGIN
    IF EXISTS (
      SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relname = 'enf_case_id_seq' AND n.nspname = 'enforcement'
    ) THEN
      ALTER SEQUENCE enforcement.enf_case_id_seq SET SCHEMA public;
    END IF;
  END $$;

DO $$
  BEGIN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'enf_case'
    ) THEN
      RAISE EXCEPTION 'Migration failed: enf_case table not found in public schema';
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.tables WHERE table_schema = 'enforcement' AND table_name = 'enf_case'
    ) THEN
      RAISE EXCEPTION 'Migration failed: enf_case table still exists in enforcement schema';
    END IF;
  END $$;

-- Drop the enforcement schema
DROP SCHEMA IF EXISTS enforcement CASCADE;

-- Verify
DO $$
  BEGIN
    IF EXISTS (
      SELECT 1 FROM information_schema.schemata
      WHERE schema_name = 'enforcement'
    ) THEN
      RAISE EXCEPTION 'Migration failed: enforcement schema still exists';
    END IF;
  END $$;
