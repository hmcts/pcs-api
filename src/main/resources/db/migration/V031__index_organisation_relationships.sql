CREATE INDEX idx_claim_party_organisation_party_id
  ON public.claim_party_organisation (party_id);

CREATE INDEX idx_claim_party_organisation_organisation_id
  ON public.claim_party_organisation (organisation_id);

CREATE INDEX idx_claim_party_contact_details_organisation_case
  ON public.claim_party_contact_details (organisation_id, case_id, id DESC);
