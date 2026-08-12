create TABLE legal_representative_organisation (
  id uuid primary key,
  organisation_id NOT NULL varchar(64),
  organisation_profile_id NOT NULL varchar(64),
  organisation_name varchar(120),
  email_address varchar(120),
  phone_number varchar(40),
  contact_reference varchar(80),
  address_id  integer REFERENCES address (id),
  has_amended_contact_details YES_NO
);

create table party_legal_rep_org (
  party_id UUID NOT NULL REFERENCES party (id),
  legal_representative_organisation_id UUID NOT NULL REFERENCES legal_representative_org (id),
  active NOT NULL YES_NO DEFAULT 'YES',
  start_date NOT NULL TIMESTAMP WITHOUT TIME ZONE,
  end_date TIMESTAMP WITHOUT TIME ZONE,

  primary key (party_id, legal_representative_organisation_id)
);
