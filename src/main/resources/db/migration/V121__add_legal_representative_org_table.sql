create TABLE legal_representative_org (
  id uuid primary key,
  organisation_id varchar(80),
  case_id UUID NOT NULL REFERENCES public.pcs_case (id),
  organisation_name varchar(120),
  email varchar(120),
  phone varchar(40),
  contact_reference varchar(80),
  address_id  UUID REFERENCES address (id),
  has_amended_contact_details YES_NO
);

create table party_legal_rep_org (
  party_id UUID REFERENCES party (id),
  legal_representative_organisation_id UUID REFERENCES legal_representative_org (id),
  active YES_NO,
  start_date TIMESTAMP WITHOUT TIME ZONE,
  end_date TIMESTAMP WITHOUT TIME ZONE,

  primary key (party_id, legal_representative_organisation_id)
);
