-- Add prohibitedConduct JSONB column to claim table
ALTER TABLE public.claim
ADD COLUMN prohibitedConduct JSONB;
