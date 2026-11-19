ALTER TABLE public.claim
    ADD COLUMN submission_channel text NOT NULL DEFAULT 'UI' CHECK (submission_channel IN ('UI', 'BULK_API')),
    ADD COLUMN claimant_designation text,
    ADD COLUMN legacy_pcol_claimant_reference text,
    ADD COLUMN legacy_pcol_solicitor_reference text,
    ADD COLUMN solicitor_firm_name text;
