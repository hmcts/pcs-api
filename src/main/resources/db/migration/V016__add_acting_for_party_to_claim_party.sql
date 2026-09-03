ALTER TABLE public.claim_party
    ADD COLUMN acting_for_party_id uuid REFERENCES public.party(id);
