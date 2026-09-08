ALTER TABLE public.counter_claim
ADD COLUMN rank integer;

WITH ranked_claims AS (
    SELECT 
        id,
        ROW_NUMBER() OVER (
            PARTITION BY case_id 
            ORDER BY 
                claim_submitted_date NULLS LAST,
                id
        ) as new_rank
    FROM public.counter_claim
)
UPDATE public.counter_claim cc
SET rank = rc.new_rank
FROM ranked_claims rc
WHERE cc.id = rc.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM counter_claim WHERE rank IS NULL) THEN
        RAISE EXCEPTION 'Some counterclaims still have NULL rank!';
    END IF;
END $$;

ALTER TABLE public.counter_claim
ALTER COLUMN rank SET NOT NULL;