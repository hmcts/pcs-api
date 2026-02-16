-- Add prohibited_conduct JSONB column to claim table
ALTER TABLE public.claim
ADD COLUMN prohibited_conduct JSONB;
