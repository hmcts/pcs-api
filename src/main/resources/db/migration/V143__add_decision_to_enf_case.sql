-- The caseworker's decision on an enforcement application (decideEnforcement).
-- enforcement_number: the warrant or writ number given on approval. Its format is still to be agreed
--   (HDPI-7352, HDPI-7353), so EnforcementNumberGenerator builds a placeholder from enf_case_number_seq.
-- approved_at: when the application was approved. For a writ this is the date of transfer to the High Court.
-- rejection_reason: why the caseworker rejected the application.
CREATE SEQUENCE public.enf_case_number_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

ALTER TABLE public.enf_case
    ADD COLUMN enforcement_number TEXT,
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN rejection_reason TEXT,
    ADD CONSTRAINT enf_case_enforcement_number_key UNIQUE (enforcement_number);
