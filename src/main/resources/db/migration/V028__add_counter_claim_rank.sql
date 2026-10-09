-- Add nullable rank column to counter_claim table
ALTER TABLE public.counter_claim
ADD COLUMN rank integer;

-- Note: Backfill of existing data and NOT NULL constraint will be added in a future migration
-- after the application code that sets rank on new counterclaims is deployed.