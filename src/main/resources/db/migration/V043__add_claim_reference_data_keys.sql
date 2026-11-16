ALTER TABLE public.claim
  ADD COLUMN claim_type_key VARCHAR(64) CONSTRAINT claim_claim_type_key_fk REFERENCES public.claim_type (key);

ALTER TABLE public.claim_ground
  ADD COLUMN grounds_type_key VARCHAR(64) CONSTRAINT claim_ground_grounds_type_key_fk REFERENCES public.ground_type (key),
  ADD COLUMN grounds_subtype_key VARCHAR(64) CONSTRAINT claim_ground_grounds_subtype_key_fk REFERENCES public.ground_subtype (key);

ALTER TABLE public.tenancy_licence
  ADD COLUMN tenancy_type_key VARCHAR(64) CONSTRAINT tenancy_licence_tenancy_type_key_fk REFERENCES public.tenancy_type (key);

ALTER TABLE public.claim_party
  ADD COLUMN entity_role_code_key VARCHAR(64) CONSTRAINT claim_party_entity_role_code_key_fk REFERENCES public.entity_role_code (key),
  ADD COLUMN claimant_type_key VARCHAR(64) CONSTRAINT claim_party_claimant_type_key_fk REFERENCES public.claimant_type (key);
