-- Shown on the Enforcement status and Risk assessment tabs (HDPI-7350, HDPI-7351).
--
-- submitted_at: when the claimant submitted the application, which dates their risk answers. Existing rows
--   stay null: the submission time was never recorded.
-- claimant_can_attend: the claimant's answer to "Can you attend the eviction on this date?" in Confirm the
--   eviction details, for the current eviction date. Null until they answer, and cleared when the eviction
--   is rescheduled.
ALTER TABLE public.enf_case
    ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN claimant_can_attend BOOLEAN;
