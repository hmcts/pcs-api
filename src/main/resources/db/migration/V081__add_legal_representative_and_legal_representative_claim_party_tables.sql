create table legal_representative (
  id uuid primary key,
  idam_id  UUID,
  organisation_name varchar(120),
  first_name varchar(60),
  last_name varchar(60),
  email varchar(120),
  phone varchar(40),
  address_id  UUID REFERENCES address (id)
);

create table claim_party_legal_representative (
  party_id UUID REFERENCES party (id),
  legal_representative_id UUID REFERENCES legal_representative (id),
  active YES_NO,
  start_date TIMESTAMP WITHOUT TIME ZONE,
  end_date TIMESTAMP WITHOUT TIME ZONE,

  primary key (party_id, legal_representative_id)
);


