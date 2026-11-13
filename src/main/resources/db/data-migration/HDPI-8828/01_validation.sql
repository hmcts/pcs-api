\set ON_ERROR_STOP on
\ir 00_work.sql

WITH :work
SELECT target,
       count(*) AS total_rows,
       count(*) FILTER (WHERE legacy_value IS NOT NULL) AS eligible_rows,
       count(*) FILTER (WHERE legacy_value IS NULL) AS no_legacy_value,
       count(*) FILTER (WHERE current_key IS NOT NULL OR current_subkey IS NOT NULL) AS already_holding_a_key,
       count(*) FILTER (WHERE status = 'ALREADY_SET') AS already_correct,
       count(*) FILTER (WHERE status = 'UPDATE') AS will_update,
       count(*) FILTER (WHERE status = 'UNMAPPED_LEGACY_VALUE') AS no_verified_mapping,
       count(*) FILTER (WHERE status = 'CONFLICTING_EXISTING_KEY') AS conflicting_existing_key,
       count(*) FILTER (WHERE status = 'REFERENCE_MISSING') AS reference_missing,
       count(*) FILTER (WHERE status = 'REFERENCE_INACTIVE') AS reference_inactive,
       count(*) FILTER (WHERE status = 'REFERENCE_MISMATCH') AS reference_mismatch
FROM classified
GROUP BY target
ORDER BY target;

WITH :work
SELECT target, scope, legacy_value, status, ref_key AS candidate_key, subtype_key AS candidate_subtype_key,
       count(*) AS row_count
FROM classified
WHERE legacy_value IS NOT NULL
GROUP BY target, scope, legacy_value, status, ref_key, subtype_key
ORDER BY target, scope, legacy_value, status;

WITH :work
SELECT cw.target, cw.scope, cw.legacy_value, cw.ref_key, cw.subtype_key,
       CASE
         WHEN rk.key IS NULL OR (cw.subtype_key IS NOT NULL AND rs.key IS NULL) THEN 'REFERENCE_MISSING'
         WHEN rk.active IS DISTINCT FROM 'YES' OR (cw.subtype_key IS NOT NULL AND rs.active IS DISTINCT FROM 'YES') THEN 'REFERENCE_INACTIVE'
         WHEN lower(regexp_replace(rk.value_en, '[^[:alnum:]]+', '', 'g'))
                <> lower(regexp_replace(cw.ref_value_en, '[^[:alnum:]]+', '', 'g'))
           OR (cw.subtype_key IS NOT NULL
               AND (lower(regexp_replace(rs.value_en, '[^[:alnum:]]+', '', 'g'))
                      <> lower(regexp_replace(cw.subtype_value_en, '[^[:alnum:]]+', '', 'g'))
                    OR rs.parent_key IS DISTINCT FROM cw.ref_key))
           THEN 'REFERENCE_MISMATCH'
         ELSE 'OK'
       END AS reference_status
FROM crosswalk cw
LEFT JOIN reference_rows rk ON rk.target = cw.target AND rk.slot = 'key' AND rk.key = cw.ref_key
LEFT JOIN reference_rows rs ON rs.target = cw.target AND rs.slot = 'subkey' AND rs.key = cw.subtype_key
WHERE rk.key IS NULL OR (cw.subtype_key IS NOT NULL AND rs.key IS NULL)
   OR rk.active IS DISTINCT FROM 'YES' OR (cw.subtype_key IS NOT NULL AND rs.active IS DISTINCT FROM 'YES')
   OR lower(regexp_replace(rk.value_en, '[^[:alnum:]]+', '', 'g'))
        <> lower(regexp_replace(cw.ref_value_en, '[^[:alnum:]]+', '', 'g'))
   OR (cw.subtype_key IS NOT NULL
       AND (lower(regexp_replace(rs.value_en, '[^[:alnum:]]+', '', 'g'))
              <> lower(regexp_replace(cw.subtype_value_en, '[^[:alnum:]]+', '', 'g'))
            OR rs.parent_key IS DISTINCT FROM cw.ref_key))
ORDER BY cw.target, cw.scope, cw.legacy_value;

SELECT 'claimant_type' AS reference_table, count(*) AS row_count, count(*) FILTER (WHERE active = 'YES') AS active_rows FROM public.claimant_type
UNION ALL SELECT 'claim_type', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.claim_type
UNION ALL SELECT 'entity_role_code', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.entity_role_code
UNION ALL SELECT 'ground_type', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.ground_type
UNION ALL SELECT 'ground_subtype', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.ground_subtype
UNION ALL SELECT 'hearing_type', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.hearing_type
UNION ALL SELECT 'tenancy_type', count(*), count(*) FILTER (WHERE active = 'YES') FROM public.tenancy_type
ORDER BY reference_table;

SELECT 'claim_party.claimant_type_key' AS key_column, count(claimant_type_key) AS populated, count(*) AS total_rows FROM public.claim_party
UNION ALL SELECT 'claim_party.entity_role_code_key', count(entity_role_code_key), count(*) FROM public.claim_party
UNION ALL SELECT 'tenancy_licence.tenancy_type_key', count(tenancy_type_key), count(*) FROM public.tenancy_licence
UNION ALL SELECT 'hearing.hearing_type_key', count(hearing_type_key), count(*) FROM public.hearing
UNION ALL SELECT 'hearing.case_management_cancellation_reason_key', count(case_management_cancellation_reason_key), count(*) FROM public.hearing
UNION ALL SELECT 'claim_ground.grounds_type_key', count(grounds_type_key), count(*) FROM public.claim_ground
UNION ALL SELECT 'claim_ground.grounds_subtype_key', count(grounds_subtype_key), count(*) FROM public.claim_ground
UNION ALL SELECT 'claim.claim_type_key', count(claim_type_key), count(*) FROM public.claim
ORDER BY key_column;

SELECT 'claim_party' AS table_name, count(*) AS row_count,
       md5(coalesce(string_agg(j::text, '' ORDER BY j::text), '')) AS checksum_excluding_key_columns
FROM (SELECT to_jsonb(t) - 'entity_role_code_key' - 'claimant_type_key' AS j FROM public.claim_party t) x
UNION ALL
SELECT 'tenancy_licence', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'tenancy_type_key' AS j FROM public.tenancy_licence t) x
UNION ALL
SELECT 'hearing', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'hearing_type_key' - 'case_management_cancellation_reason_key' AS j FROM public.hearing t) x
UNION ALL
SELECT 'claim_ground', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'grounds_type_key' - 'grounds_subtype_key' AS j FROM public.claim_ground t) x
UNION ALL
SELECT 'claim', count(*), md5(coalesce(string_agg(j::text, '' ORDER BY j::text), ''))
FROM (SELECT to_jsonb(t) - 'claim_type_key' AS j FROM public.claim t) x
ORDER BY table_name;

SELECT event_object_table AS table_name, trigger_name, event_manipulation, action_timing
FROM information_schema.triggers
WHERE event_object_schema = 'public'
  AND event_object_table IN ('claim_party', 'tenancy_licence', 'hearing', 'claim_ground')
ORDER BY event_object_table, trigger_name;
