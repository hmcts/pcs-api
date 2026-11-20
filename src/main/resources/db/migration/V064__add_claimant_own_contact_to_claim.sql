ALTER TABLE public.claim
    ADD COLUMN claimant_own_address_id integer REFERENCES public.address(id),
    ADD COLUMN claimant_own_telephone_number text,
    ADD COLUMN claimant_own_email_address text;
