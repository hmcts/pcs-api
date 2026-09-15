-- This reverts the changes made in V028
ALTER TABLE public.counter_claim
DROP COLUMN IF EXISTS rank;
