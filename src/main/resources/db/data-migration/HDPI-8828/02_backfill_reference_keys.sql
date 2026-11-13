\set ON_ERROR_STOP on
\ir 00_work.sql

BEGIN;

SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '30min';

CREATE TEMP TABLE hdpi8828_work ON COMMIT DROP AS
WITH :work
SELECT target, row_id, ref_key, subtype_key, status FROM classified;

CREATE TEMP TABLE hdpi8828_result (target text PRIMARY KEY, planned bigint NOT NULL, updated bigint NOT NULL) ON COMMIT DROP;

WITH updated AS (
  UPDATE public.claim_party cp
  SET claimant_type_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'claim_party.claimant_type_key'
    AND w.status = 'UPDATE'
    AND w.row_id = cp.claim_id::text || '/' || cp.party_id::text
    AND cp.role = 'CLAIMANT'
    AND cp.claimant_type_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'claim_party.claimant_type_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'claim_party.claimant_type_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.claim_party cp
  SET entity_role_code_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'claim_party.entity_role_code_key'
    AND w.status = 'UPDATE'
    AND w.row_id = cp.claim_id::text || '/' || cp.party_id::text
    AND cp.entity_role_code_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'claim_party.entity_role_code_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'claim_party.entity_role_code_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.tenancy_licence tl
  SET tenancy_type_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'tenancy_licence.tenancy_type_key'
    AND w.status = 'UPDATE'
    AND w.row_id = tl.id::text
    AND tl.tenancy_type_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'tenancy_licence.tenancy_type_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'tenancy_licence.tenancy_type_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.hearing h
  SET hearing_type_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'hearing.hearing_type_key'
    AND w.status = 'UPDATE'
    AND w.row_id = h.id::text
    AND h.hearing_type_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'hearing.hearing_type_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'hearing.hearing_type_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.hearing h
  SET case_management_cancellation_reason_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'hearing.case_management_cancellation_reason_key'
    AND w.status = 'UPDATE'
    AND w.row_id = h.id::text
    AND h.case_management_cancellation_reason_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'hearing.case_management_cancellation_reason_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'hearing.case_management_cancellation_reason_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.claim c
  SET claim_type_key = w.ref_key
  FROM hdpi8828_work w
  WHERE w.target = 'claim.claim_type_key'
    AND w.status = 'UPDATE'
    AND w.row_id = c.id::text
    AND c.claim_type_key IS NULL
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'claim.claim_type_key',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'claim.claim_type_key' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

WITH updated AS (
  UPDATE public.claim_ground g
  SET grounds_type_key = coalesce(g.grounds_type_key, w.ref_key),
      grounds_subtype_key = coalesce(g.grounds_subtype_key, w.subtype_key)
  FROM hdpi8828_work w
  WHERE w.target = 'claim_ground'
    AND w.status = 'UPDATE'
    AND w.row_id = g.id::text
    AND (g.grounds_type_key IS NULL OR g.grounds_type_key = w.ref_key)
    AND (g.grounds_subtype_key IS NULL OR g.grounds_subtype_key IS NOT DISTINCT FROM w.subtype_key)
  RETURNING 1
)
INSERT INTO hdpi8828_result
SELECT 'claim_ground',
       (SELECT count(*) FROM hdpi8828_work WHERE target = 'claim_ground' AND status = 'UPDATE'),
       (SELECT count(*) FROM updated);

SELECT target, planned, updated FROM hdpi8828_result ORDER BY target;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM hdpi8828_result WHERE planned <> updated) THEN
    RAISE EXCEPTION 'HDPI-8828: updated row counts differ from the validated plan; rolling back';
  END IF;
END
$$;

\if :{?apply}
  \if :apply
COMMIT;
  \else
ROLLBACK;
  \endif
\else
ROLLBACK;
\endif
