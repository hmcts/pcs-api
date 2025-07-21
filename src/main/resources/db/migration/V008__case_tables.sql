create table address (
  id            uuid primary key,
  version       integer,
  address_line1 varchar(100),
  address_line2 varchar(100),
  address_line3 integer,
  post_town     varchar(100),
  county        varchar(100),
  postcode      varchar(10),
  country       varchar(100)
);

create table pcs_case (
  id                  uuid primary key,
  version             integer,
  case_reference      bigint unique,
  applicant_forename  varchar(100),
  applicant_surname   varchar(100),
  property_address_id uuid references address (id)
);
