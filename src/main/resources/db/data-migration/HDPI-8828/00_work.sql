SELECT $work$
crosswalk (target, scope, legacy_value, ref_key, ref_value_en, subtype_key, subtype_value_en) AS (
  VALUES
    ('claim_party.claimant_type_key', '', 'PROVIDER_OF_SOCIAL_HOUSING', 'AAA3-SHS', 'Social Housing', NULL, NULL),
    ('claim_party.claimant_type_key', '', 'COMMUNITY_LANDLORD', 'AAA3-RCL', 'Registered Community Landlord', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'CLAIMANT', 'CLAI', 'Claimant', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'DEFENDANT', 'DEFE', 'Defendant', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'ASSURED_TENANCY', 'AAA3-ATY', 'Assured tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'SECURE_TENANCY', 'AAA3-STY', 'Secure tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'FLEXIBLE_TENANCY', 'AAA3-FTY', 'Flexible tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'SECURE_CONTRACT', 'AAA3-SEC', 'Secure contract', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'OTHER', 'AAA3-OTH', 'Other', NULL, NULL),
    ('hearing.hearing_type_key', '', 'POSSESSION', 'AAA3-FPH', 'First Possession Hearing', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|RENT_ARREARS', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', 'AAA3-sub-grounds-001', 'Rent arrears'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|BREACH', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', 'AAA3-sub-grounds-002', 'Breach of tenancy'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|BOTH', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_MANDATORY_ALT', 'LANDLORD_WORKS', 'AAA3-grounds-028', 'Ground 10: Landlord''s works', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'OWNER_OCCUPIER_GROUND1', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'SERIOUS_RENT_ARREARS_GROUND8', 'AAA3-grounds-001', 'Serious rent arrears (ground 8)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'RENT_ARREARS_GROUND10', 'AAA3-grounds-038', 'Any rent arrears (ground 10)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'PERSISTENT_DELAY_GROUND11', 'AAA3-grounds-039', 'Persistent arrears (ground 11)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'BREACH_TENANCY_GROUND12', 'AAA3-grounds-040', 'Breach of tenancy (ground 12)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'DETERIORATION_PROPERTY_GROUND13', 'AAA3-grounds-041', 'Deterioration of property (ground 13)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'NUISANCE_ANNOYANCE_GROUND14', 'AAA3-grounds-042', 'Anti-social behaviour (ground 14)', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'BREACH_OF_THE_TENANCY', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'ANTI_SOCIAL', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'OTHER', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS|OTHER', 'NO_GROUNDS', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'RENT_ARREARS_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'ANTISOCIAL_BEHAVIOUR_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'OTHER_BREACH_OF_CONTRACT_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim.claim_type_key', '', 'PROVIDER_OF_SOCIAL_HOUSING|ENGLAND', 'AAA3-SLD', 'Standard Possession Claim (Social Landlord - England)', NULL, NULL),
    ('claim.claim_type_key', '', 'COMMUNITY_LANDLORD|WALES', 'AAA3-WSL', 'Standard Possession Claim (Social Landlord - Wales)', NULL, NULL)
),
tenancy_of_claim AS (
  SELECT c.id AS claim_id,
         CASE
           WHEN count(tl.id) = 0 THEN 'NO_TENANCY'
           WHEN count(tl.id) FILTER (WHERE nullif(btrim(tl.type), '') IS NULL) > 0 THEN 'AMBIGUOUS_TENANCY_TYPES'
           WHEN count(DISTINCT btrim(tl.type)) = 1 THEN min(btrim(tl.type))
           ELSE 'AMBIGUOUS_TENANCY_TYPES'
         END AS tenancy_type
  FROM public.claim c
  LEFT JOIN public.tenancy_licence tl ON tl.case_id = c.case_id
  GROUP BY c.id
),
source_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target,
         cp.claim_id::text || '/' || cp.party_id::text AS row_id,
         '' AS scope,
         nullif(btrim(c.claimant_type), '') AS legacy_value,
         cp.claimant_type_key::text AS current_key,
         NULL::text AS current_subkey
  FROM public.claim_party cp
  JOIN public.claim c ON c.id = cp.claim_id
  WHERE btrim(cp.role) = 'CLAIMANT'
  UNION ALL
  SELECT 'claim_party.entity_role_code_key', claim_id::text || '/' || party_id::text, '',
         nullif(btrim(role), ''), entity_role_code_key::text, NULL::text
  FROM public.claim_party
  UNION ALL
  SELECT 'tenancy_licence.tenancy_type_key', id::text, '',
         nullif(btrim(type), ''), tenancy_type_key::text, NULL::text
  FROM public.tenancy_licence
  UNION ALL
  SELECT 'hearing.hearing_type_key', id::text, '',
         nullif(btrim(type), ''), hearing_type_key::text, NULL::text
  FROM public.hearing
  UNION ALL
  SELECT 'claim_ground', g.id::text,
         CASE
           WHEN btrim(g.code) = 'RENT_ARREARS_OR_BREACH_OF_TENANCY'
             THEN btrim(g.category) || '|' || CASE
               WHEN coalesce(g.is_rent_arrears, false) AND nullif(btrim(g.reason), '') IS NULL THEN 'RENT_ARREARS'
               WHEN NOT coalesce(g.is_rent_arrears, false) AND nullif(btrim(g.reason), '') IS NOT NULL THEN 'BREACH'
               WHEN coalesce(g.is_rent_arrears, false) THEN 'BOTH'
               ELSE 'NEITHER'
             END
           WHEN btrim(g.category) LIKE 'INTRODUCTORY_DEMOTED_OTHER%'
             THEN btrim(g.category) || '|' || coalesce(t.tenancy_type, 'NO_TENANCY')
           ELSE btrim(g.category)
         END,
         nullif(btrim(g.code), ''), g.grounds_type_key::text, g.grounds_subtype_key::text
  FROM public.claim_ground g
  LEFT JOIN tenancy_of_claim t ON t.claim_id = g.claim_id
  UNION ALL
  SELECT 'claim.claim_type_key', c.id::text, '',
         CASE
           WHEN c.against_trespassers::text = 'YES' THEN 'TRESPASS'
           WHEN btrim(c.claimant_type) = 'MORTGAGE_LENDER' THEN 'MORTGAGE_LENDER'
           WHEN btrim(c.claimant_type) IN ('PROVIDER_OF_SOCIAL_HOUSING', 'COMMUNITY_LANDLORD', 'PRIVATE_LANDLORD')
                AND pc.legislative_country IN ('ENGLAND', 'WALES')
             THEN btrim(c.claimant_type) || '|' || pc.legislative_country
           ELSE nullif(btrim(c.claimant_type), '')
         END,
         c.claim_type_key::text, NULL::text
  FROM public.claim c
  LEFT JOIN public.pcs_case pc ON pc.id = c.case_id
),
reference_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target, 'key' AS slot, key, value_en, active, NULL::text AS parent_key
  FROM public.claimant_type
  UNION ALL SELECT 'claim_party.entity_role_code_key', 'key', key, value_en, active, NULL FROM public.entity_role_code
  UNION ALL SELECT 'tenancy_licence.tenancy_type_key', 'key', key, value_en, active, NULL FROM public.tenancy_type
  UNION ALL SELECT 'hearing.hearing_type_key', 'key', key, value_en, active, NULL FROM public.hearing_type
  UNION ALL SELECT 'claim_ground', 'key', key, value_en, active, NULL FROM public.ground_type
  UNION ALL SELECT 'claim_ground', 'subkey', key, value_en, active, parent_key FROM public.ground_subtype
  UNION ALL SELECT 'claim.claim_type_key', 'key', key, value_en, active, NULL FROM public.claim_type
),
classified AS (
  SELECT s.target, s.row_id, s.scope, s.legacy_value, s.current_key, s.current_subkey,
         cw.ref_key, cw.subtype_key,
         CASE
           WHEN s.legacy_value IS NULL THEN 'NO_LEGACY_VALUE'
           WHEN cw.ref_key IS NULL THEN 'UNMAPPED_LEGACY_VALUE'
           WHEN (s.current_key IS NOT NULL AND s.current_key <> cw.ref_key)
             OR (s.current_subkey IS NOT NULL AND s.current_subkey IS DISTINCT FROM cw.subtype_key)
             THEN 'CONFLICTING_EXISTING_KEY'
           WHEN s.current_key = cw.ref_key AND s.current_subkey IS NOT DISTINCT FROM cw.subtype_key
             THEN 'ALREADY_SET'
           WHEN rk.key IS NULL OR (cw.subtype_key IS NOT NULL AND rs.key IS NULL) THEN 'REFERENCE_MISSING'
           WHEN rk.active IS DISTINCT FROM 'YES' OR (cw.subtype_key IS NOT NULL AND rs.active IS DISTINCT FROM 'YES') THEN 'REFERENCE_INACTIVE'
           WHEN lower(regexp_replace(rk.value_en, '[^[:alnum:]]+', '', 'g')) <> lower(regexp_replace(cw.ref_value_en, '[^[:alnum:]]+', '', 'g'))
             OR (cw.subtype_key IS NOT NULL AND (lower(regexp_replace(rs.value_en, '[^[:alnum:]]+', '', 'g')) <> lower(regexp_replace(cw.subtype_value_en, '[^[:alnum:]]+', '', 'g')) OR rs.parent_key IS DISTINCT FROM cw.ref_key))
             THEN 'REFERENCE_MISMATCH'
           ELSE 'UPDATE'
         END AS status
  FROM source_rows s
  LEFT JOIN crosswalk cw
    ON cw.target = s.target AND cw.scope = s.scope AND cw.legacy_value = s.legacy_value
  LEFT JOIN reference_rows rk
    ON rk.target = s.target AND rk.slot = 'key' AND rk.key = cw.ref_key
  LEFT JOIN reference_rows rs
    ON rs.target = s.target AND rs.slot = 'subkey' AND rs.key = cw.subtype_key
)
$work$ AS work
\gset
