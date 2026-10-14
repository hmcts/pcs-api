-- Number existing counterclaims per case, earliest submission first.
UPDATE public.counter_claim AS counter_claim
SET rank = ranked.calculated_rank
FROM (
    SELECT id,
           CAST(ROW_NUMBER() OVER (
               PARTITION BY case_id
               ORDER BY claim_submitted_date, id
           ) AS integer) AS calculated_rank
    FROM public.counter_claim
) AS ranked
WHERE counter_claim.id = ranked.id
  AND counter_claim.rank IS NULL;
