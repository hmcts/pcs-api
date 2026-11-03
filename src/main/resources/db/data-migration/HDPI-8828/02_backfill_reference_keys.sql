-- HDPI-8828 one-time backfill of reference-data keys. Not a Flyway migration: run manually after the reference tables are populated.
-- Run 01_validation.sql before and after. Replace COMMIT with ROLLBACK for a dry run. Safe to re-run.
-- Only rows whose key is NULL, whose legacy value has a crosswalk entry and whose reference row exists are updated.

BEGIN;

WITH updated AS (
  UPDATE public.claim_party AS claim_party
  SET claimant_type_key = ref.key
  FROM public.claim AS claim
  JOIN (VALUES
    ('PRIVATE_LANDLORD', 'AAA3-PLD'),
    ('PROVIDER_OF_SOCIAL_HOUSING', 'AAA3-SHS'),
    ('COMMUNITY_LANDLORD', 'AAA3-RCL'),
    ('MORTGAGE_LENDER', 'AAA3-MPL'),
    ('OTHER', 'AAA3-OTH')
  ) AS crosswalk (legacy_value, ref_key) ON crosswalk.legacy_value = btrim(claim.claimant_type)
  JOIN public.claimant_type AS ref ON ref.key = crosswalk.ref_key
  WHERE claim_party.claim_id = claim.id
    AND claim_party.role = 'CLAIMANT'
    AND claim_party.claimant_type_key IS NULL
  RETURNING 1
)
SELECT 'claim_party.claimant_type_key' AS target, count(*) AS rows_updated FROM updated;

WITH updated AS (
  UPDATE public.claim_party AS claim_party
  SET entity_role_code_key = ref.key
  FROM (VALUES
    ('CLAIMANT', 'CLAI'),
    ('DEFENDANT', 'DEFE'),
    ('LITIGATION_FRIEND', 'LIFR')
  ) AS crosswalk (legacy_value, ref_key)
  JOIN public.entity_role_code AS ref ON ref.key = crosswalk.ref_key
  WHERE crosswalk.legacy_value = btrim(claim_party.role)
    AND claim_party.entity_role_code_key IS NULL
  RETURNING 1
)
SELECT 'claim_party.entity_role_code_key' AS target, count(*) AS rows_updated FROM updated;

WITH updated AS (
  UPDATE public.tenancy_licence AS tenancy_licence
  SET tenancy_type_key = ref.key
  FROM (VALUES
    ('ASSURED_TENANCY', 'AAA3-ATY'),
    ('SECURE_TENANCY', 'AAA3-STY'),
    ('INTRODUCTORY_TENANCY', 'AAA3-ITY'),
    ('FLEXIBLE_TENANCY', 'AAA3-FTY'),
    ('DEMOTED_TENANCY', 'AAA3-DTY'),
    ('SECURE_CONTRACT', 'AAA3-SEC'),
    ('STANDARD_CONTRACT', 'AAA3-STN'),
    ('OTHER', 'AAA3-OTH')
  ) AS crosswalk (legacy_value, ref_key)
  JOIN public.tenancy_type AS ref ON ref.key = crosswalk.ref_key
  WHERE crosswalk.legacy_value = btrim(tenancy_licence.type)
    AND tenancy_licence.tenancy_type_key IS NULL
  RETURNING 1
)
SELECT 'tenancy_licence.tenancy_type_key' AS target, count(*) AS rows_updated FROM updated;

WITH updated AS (
  UPDATE public.hearing AS hearing
  SET hearing_type_key = ref.key
  FROM (VALUES
    ('POSSESSION', 'AAA3-FPH'),
    ('APPLICATION', 'AAA3-APP'),
    ('ADJOURNED', 'AAA3-AFP')
  ) AS crosswalk (legacy_value, ref_key)
  JOIN public.hearing_type AS ref ON ref.key = crosswalk.ref_key
  WHERE crosswalk.legacy_value = btrim(hearing.type)
    AND hearing.hearing_type_key IS NULL
  RETURNING 1
)
SELECT 'hearing.hearing_type_key' AS target, count(*) AS rows_updated FROM updated;

COMMIT;
