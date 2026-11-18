ALTER TABLE public.party
  ADD COLUMN entity_role_code_key VARCHAR(64) CONSTRAINT party_entity_role_code_key_fk REFERENCES public.entity_role_code (key);

ALTER TABLE public.claim_party
  DROP COLUMN entity_role_code_key;
