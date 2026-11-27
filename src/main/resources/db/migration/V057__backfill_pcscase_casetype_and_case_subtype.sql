UPDATE public.pcs_case pc
SET case_type_key    = k.type_key,
    case_subtype_key = k.type_key
FROM (
  SELECT c.case_id,
         CASE
           WHEN bool_or(EXISTS (
                  SELECT 1
                  FROM public.claim_ground cg
                  WHERE cg.claim_id = c.id
                    AND cg.is_rent_arrears
                ))
           THEN 'AAA3-RA-SL'
           ELSE 'AAA3-RNA-SL'
         END AS type_key
  FROM public.claim c
  WHERE c.claimant_type IN ('PROVIDER_OF_SOCIAL_HOUSING', 'COMMUNITY_LANDLORD')
  GROUP BY c.case_id
) k,
ccd.case_data cd
WHERE pc.id = k.case_id
  AND cd.reference = pc.case_reference
  AND cd.state IN ('PENDING_CASE_ISSUED', 'CASE_ISSUED')
  AND pc.case_type_key IS NULL;