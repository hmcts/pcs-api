create table legal_representative_organisation (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  organisation_id VARCHAR(64) NOT NULL,
  organisation_profile_id VARCHAR(64) NOT NULL,
  organisation_name VARCHAR(120),
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

create table claim_party_legal_representative_org (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  party_id UUID NOT NULL,
  legal_representative_organisation_id INTEGER NOT NULL,
  active YES_NO NOT NULL DEFAULT 'YES',
  start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  end_date TIMESTAMP WITHOUT TIME ZONE
);

create table legal_rep_org_contact_details (
  id integer NOT NULL GENERATED ALWAYS AS IDENTITY,
  case_id UUID,
  legal_rep_org_id INTEGER NOT NULL,
  email_address VARCHAR(120),
  phone_number VARCHAR(40),
  contact_reference VARCHAR(80),
  address_id INTEGER,
  contact_details_correct_confirmation YES_NO
);

ALTER TABLE ONLY public.legal_representative_organisation
  ADD CONSTRAINT legal_representative_organisation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.claim_party_legal_representative_org
  ADD CONSTRAINT claim_party_legal_representative_org_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.legal_rep_org_contact_details
  ADD CONSTRAINT legal_rep_org_contact_details_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.claim_party_legal_representative_org
  ADD CONSTRAINT claim_party_legal_representative_org_lro_id_fkey FOREIGN KEY (legal_representative_organisation_id) REFERENCES public.legal_representative_organisation(id);

ALTER TABLE ONLY public.claim_party_legal_representative_org
  ADD CONSTRAINT claim_party_legal_representative_org_party_id_fkey FOREIGN KEY (party_id) REFERENCES public.party(id);

ALTER TABLE ONLY public.legal_rep_org_contact_details
  ADD CONSTRAINT legal_rep_org_contact_details_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.pcs_case(id);

ALTER TABLE ONLY public.legal_rep_org_contact_details
  ADD CONSTRAINT legal_rep_org_contact_details_lro_id_fkey FOREIGN KEY (legal_rep_org_id) REFERENCES public.legal_representative_organisation(id);

ALTER TABLE ONLY public.legal_rep_org_contact_details
  ADD CONSTRAINT legal_rep_org_contact_details_address_id_fkey FOREIGN KEY (address_id) REFERENCES public.address(id);

