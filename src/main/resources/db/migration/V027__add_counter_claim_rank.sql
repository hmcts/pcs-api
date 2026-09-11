-- Step 1: Add nullable column
ALTER TABLE public.counter_claim
ADD COLUMN rank integer;

-- Step 2: Backfill existing data
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

-- Step 3: Verify backfill succeeded
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM counter_claim WHERE rank IS NULL) THEN
        RAISE EXCEPTION 'Some counterclaims still have NULL rank after backfill!';
    END IF;
END $$;

-- Note: NOT NULL constraint will be added in a future migration
-- after the application code that sets rank on new counterclaims is deployed.
