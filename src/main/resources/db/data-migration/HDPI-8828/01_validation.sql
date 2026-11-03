-- HDPI-8828 read-only validation. Run before and after 02_backfill_reference_keys.sql and compare the results.

-- 1. Per mapping: legacy rows, already populated, candidates, expected updates and rows that cannot be matched.
WITH crosswalk (target, legacy_value, ref_key) AS (
  VALUES
    ('claim_party.claimant_type_key', 'PRIVATE_LANDLORD', 'AAA3-PLD'),
    ('claim_party.claimant_type_key', 'PROVIDER_OF_SOCIAL_HOUSING', 'AAA3-SHS'),
    ('claim_party.claimant_type_key', 'COMMUNITY_LANDLORD', 'AAA3-RCL'),
    ('claim_party.claimant_type_key', 'MORTGAGE_LENDER', 'AAA3-MPL'),
    ('claim_party.claimant_type_key', 'OTHER', 'AAA3-OTH'),
    ('claim_party.entity_role_code_key', 'CLAIMANT', 'CLAI'),
    ('claim_party.entity_role_code_key', 'DEFENDANT', 'DEFE'),
    ('claim_party.entity_role_code_key', 'LITIGATION_FRIEND', 'LIFR'),
    ('tenancy_licence.tenancy_type_key', 'ASSURED_TENANCY', 'AAA3-ATY'),
    ('tenancy_licence.tenancy_type_key', 'SECURE_TENANCY', 'AAA3-STY'),
    ('tenancy_licence.tenancy_type_key', 'INTRODUCTORY_TENANCY', 'AAA3-ITY'),
    ('tenancy_licence.tenancy_type_key', 'FLEXIBLE_TENANCY', 'AAA3-FTY'),
    ('tenancy_licence.tenancy_type_key', 'DEMOTED_TENANCY', 'AAA3-DTY'),
    ('tenancy_licence.tenancy_type_key', 'SECURE_CONTRACT', 'AAA3-SEC'),
    ('tenancy_licence.tenancy_type_key', 'STANDARD_CONTRACT', 'AAA3-STN'),
    ('tenancy_licence.tenancy_type_key', 'OTHER', 'AAA3-OTH'),
    ('hearing.hearing_type_key', 'POSSESSION', 'AAA3-FPH'),
    ('hearing.hearing_type_key', 'APPLICATION', 'AAA3-APP'),
    ('hearing.hearing_type_key', 'ADJOURNED', 'AAA3-AFP')
),
source_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target, btrim(claim.claimant_type) AS legacy_value,
         claim_party.claimant_type_key AS current_key
  FROM public.claim_party claim_party
  JOIN public.claim claim ON claim.id = claim_party.claim_id
  WHERE claim_party.role = 'CLAIMANT' AND btrim(claim.claimant_type) <> ''
  UNION ALL
  SELECT 'claim_party.entity_role_code_key', btrim(role), entity_role_code_key
  FROM public.claim_party
  WHERE btrim(role) <> ''
  UNION ALL
  SELECT 'tenancy_licence.tenancy_type_key', btrim(type), tenancy_type_key
  FROM public.tenancy_licence
  WHERE btrim(type) <> ''
  UNION ALL
  SELECT 'hearing.hearing_type_key', btrim(type), hearing_type_key
  FROM public.hearing
  WHERE btrim(type) <> ''
),
reference_keys (target, ref_key) AS (
  SELECT 'claim_party.claimant_type_key', key FROM public.claimant_type
  UNION ALL SELECT 'claim_party.entity_role_code_key', key FROM public.entity_role_code
  UNION ALL SELECT 'tenancy_licence.tenancy_type_key', key FROM public.tenancy_type
  UNION ALL SELECT 'hearing.hearing_type_key', key FROM public.hearing_type
),
classified AS (
  SELECT source_rows.*, crosswalk.ref_key AS mapped_key, reference_keys.ref_key IS NOT NULL AS reference_row_exists
  FROM source_rows
  LEFT JOIN crosswalk
    ON crosswalk.target = source_rows.target AND crosswalk.legacy_value = source_rows.legacy_value
  LEFT JOIN reference_keys
    ON reference_keys.target = crosswalk.target AND reference_keys.ref_key = crosswalk.ref_key
)
SELECT target,
       count(*) AS legacy_rows,
       count(*) FILTER (WHERE current_key IS NOT NULL) AS already_populated,
       count(*) FILTER (WHERE current_key IS NULL) AS candidates,
       count(*) FILTER (WHERE current_key IS NULL AND mapped_key IS NOT NULL AND reference_row_exists) AS expected_updates,
       count(*) FILTER (WHERE current_key IS NULL AND mapped_key IS NULL) AS unmapped_legacy_value,
       count(*) FILTER (WHERE current_key IS NULL AND mapped_key IS NOT NULL AND NOT reference_row_exists) AS reference_row_missing,
       count(*) FILTER (WHERE current_key IS NOT NULL AND mapped_key IS NOT NULL AND current_key <> mapped_key) AS populated_differs_from_mapping
FROM classified
GROUP BY target
ORDER BY target;

-- 2. Legacy values that will not be migrated, for DA investigation.
WITH crosswalk (target, legacy_value) AS (
  VALUES
    ('claim_party.claimant_type_key', 'PRIVATE_LANDLORD'),
    ('claim_party.claimant_type_key', 'PROVIDER_OF_SOCIAL_HOUSING'),
    ('claim_party.claimant_type_key', 'COMMUNITY_LANDLORD'),
    ('claim_party.claimant_type_key', 'MORTGAGE_LENDER'),
    ('claim_party.claimant_type_key', 'OTHER'),
    ('claim_party.entity_role_code_key', 'CLAIMANT'),
    ('claim_party.entity_role_code_key', 'DEFENDANT'),
    ('claim_party.entity_role_code_key', 'LITIGATION_FRIEND'),
    ('tenancy_licence.tenancy_type_key', 'ASSURED_TENANCY'),
    ('tenancy_licence.tenancy_type_key', 'SECURE_TENANCY'),
    ('tenancy_licence.tenancy_type_key', 'INTRODUCTORY_TENANCY'),
    ('tenancy_licence.tenancy_type_key', 'FLEXIBLE_TENANCY'),
    ('tenancy_licence.tenancy_type_key', 'DEMOTED_TENANCY'),
    ('tenancy_licence.tenancy_type_key', 'SECURE_CONTRACT'),
    ('tenancy_licence.tenancy_type_key', 'STANDARD_CONTRACT'),
    ('tenancy_licence.tenancy_type_key', 'OTHER'),
    ('hearing.hearing_type_key', 'POSSESSION'),
    ('hearing.hearing_type_key', 'APPLICATION'),
    ('hearing.hearing_type_key', 'ADJOURNED')
),
source_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target, claim.claimant_type AS legacy_value
  FROM public.claim_party claim_party
  JOIN public.claim claim ON claim.id = claim_party.claim_id
  WHERE claim_party.role = 'CLAIMANT' AND claim_party.claimant_type_key IS NULL AND btrim(claim.claimant_type) <> ''
  UNION ALL
  SELECT 'claim_party.entity_role_code_key', role
  FROM public.claim_party
  WHERE entity_role_code_key IS NULL AND btrim(role) <> ''
  UNION ALL
  SELECT 'tenancy_licence.tenancy_type_key', type
  FROM public.tenancy_licence
  WHERE tenancy_type_key IS NULL AND btrim(type) <> ''
  UNION ALL
  SELECT 'hearing.hearing_type_key', type
  FROM public.hearing
  WHERE hearing_type_key IS NULL AND btrim(type) <> ''
)
SELECT source_rows.target, source_rows.legacy_value, count(*) AS row_count
FROM source_rows
LEFT JOIN crosswalk
  ON crosswalk.target = source_rows.target AND crosswalk.legacy_value = btrim(source_rows.legacy_value)
WHERE crosswalk.legacy_value IS NULL
GROUP BY source_rows.target, source_rows.legacy_value
ORDER BY source_rows.target, row_count DESC, source_rows.legacy_value;

-- 3. Mapped reference keys missing from the reference tables (expected to be empty once reference data is loaded).
SELECT expected.target, expected.ref_key
FROM (
  VALUES
    ('claim_party.claimant_type_key', 'claimant_type', 'AAA3-PLD'),
    ('claim_party.claimant_type_key', 'claimant_type', 'AAA3-SHS'),
    ('claim_party.claimant_type_key', 'claimant_type', 'AAA3-RCL'),
    ('claim_party.claimant_type_key', 'claimant_type', 'AAA3-MPL'),
    ('claim_party.claimant_type_key', 'claimant_type', 'AAA3-OTH'),
    ('claim_party.entity_role_code_key', 'entity_role_code', 'CLAI'),
    ('claim_party.entity_role_code_key', 'entity_role_code', 'DEFE'),
    ('claim_party.entity_role_code_key', 'entity_role_code', 'LIFR'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-ATY'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-STY'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-ITY'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-FTY'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-DTY'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-SEC'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-STN'),
    ('tenancy_licence.tenancy_type_key', 'tenancy_type', 'AAA3-OTH'),
    ('hearing.hearing_type_key', 'hearing_type', 'AAA3-FPH'),
    ('hearing.hearing_type_key', 'hearing_type', 'AAA3-APP'),
    ('hearing.hearing_type_key', 'hearing_type', 'AAA3-AFP')
) AS expected (target, reference_table, ref_key)
WHERE NOT EXISTS (
  SELECT 1
  FROM (
    SELECT 'claimant_type' AS reference_table, key FROM public.claimant_type
    UNION ALL SELECT 'entity_role_code', key FROM public.entity_role_code
    UNION ALL SELECT 'tenancy_type', key FROM public.tenancy_type
    UNION ALL SELECT 'hearing_type', key FROM public.hearing_type
  ) AS present
  WHERE present.reference_table = expected.reference_table AND present.key = expected.ref_key
)
ORDER BY expected.target, expected.ref_key;

-- 4. Populated key columns that do not resolve to a reference row (expected 0 for every column).
SELECT 'claim_party.claimant_type_key' AS target, count(*) AS invalid_keys
FROM public.claim_party cp
WHERE cp.claimant_type_key IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.claimant_type r WHERE r.key = cp.claimant_type_key)
UNION ALL
SELECT 'claim_party.entity_role_code_key', count(*)
FROM public.claim_party cp
WHERE cp.entity_role_code_key IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.entity_role_code r WHERE r.key = cp.entity_role_code_key)
UNION ALL
SELECT 'tenancy_licence.tenancy_type_key', count(*)
FROM public.tenancy_licence tl
WHERE tl.tenancy_type_key IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.tenancy_type r WHERE r.key = tl.tenancy_type_key)
UNION ALL
SELECT 'hearing.hearing_type_key', count(*)
FROM public.hearing h
WHERE h.hearing_type_key IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.hearing_type r WHERE r.key = h.hearing_type_key);

-- 5. Row counts and a checksum of every column except the key columns. Identical before and after the backfill.
SELECT 'claim_party' AS table_name, count(*) AS row_count,
       md5(coalesce(string_agg(j::text, '' ORDER BY j::text), '')) AS checksum_excluding_keys
FROM (SELECT to_jsonb(t) - 'entity_role_code_key' - 'claimant_type_key' AS j FROM public.claim_party t) x
UNION ALL
SELECT 'tenancy_licence', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'tenancy_type_key' AS j FROM public.tenancy_licence t) x
UNION ALL
SELECT 'hearing', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'hearing_type_key' AS j FROM public.hearing t) x
UNION ALL
SELECT 'claim', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) AS j FROM public.claim t) x
UNION ALL
SELECT 'claim_ground', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) AS j FROM public.claim_ground t) x;

-- 6. Reference table sizes.
SELECT 'claimant_type' AS reference_table, count(*) AS row_count FROM public.claimant_type
UNION ALL SELECT 'entity_role_code', count(*) FROM public.entity_role_code
UNION ALL SELECT 'tenancy_type', count(*) FROM public.tenancy_type
UNION ALL SELECT 'hearing_type', count(*) FROM public.hearing_type
UNION ALL SELECT 'claim_type', count(*) FROM public.claim_type
UNION ALL SELECT 'ground_type', count(*) FROM public.ground_type
UNION ALL SELECT 'ground_subtype', count(*) FROM public.ground_subtype
ORDER BY reference_table;

-- 7. Triggers on the tables the backfill updates (review before running; none expected to modify other columns).
SELECT event_object_table AS table_name, trigger_name, event_manipulation, action_timing
FROM information_schema.triggers
WHERE event_object_schema = 'public'
  AND event_object_table IN ('claim_party', 'tenancy_licence', 'hearing')
ORDER BY event_object_table, trigger_name;

-- 8. Claim grounds still awaiting a DA-approved crosswalk (claim_ground.code to grounds_type_key and grounds_subtype_key).
SELECT category, code, count(*) AS row_count,
       count(*) FILTER (WHERE grounds_type_key IS NOT NULL) AS grounds_type_populated,
       count(*) FILTER (WHERE grounds_subtype_key IS NOT NULL) AS grounds_subtype_populated
FROM public.claim_ground
GROUP BY category, code
ORDER BY category, code;
