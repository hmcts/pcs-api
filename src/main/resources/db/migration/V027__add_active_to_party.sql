ALTER TABLE public.party
    ADD COLUMN active public.yes_no DEFAULT 'YES' NOT NULL;
