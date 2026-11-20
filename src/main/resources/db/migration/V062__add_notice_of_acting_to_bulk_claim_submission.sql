ALTER TABLE public.bulk_claim_submission
    ADD COLUMN notice_of_acting_required boolean NOT NULL DEFAULT false;
