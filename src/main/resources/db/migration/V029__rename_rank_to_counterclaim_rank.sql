-- Rename rank column to counterclaim_rank to avoid PostgreSQL reserved word
ALTER TABLE public.counter_claim
RENAME COLUMN rank TO counterclaim_rank;
