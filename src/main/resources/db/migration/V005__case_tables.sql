
create table address (
  id            uuid primary key,
  version       integer,
  address_line1 varchar(100),
  address_line2 varchar(100),
  address_line3 varchar(100),
  post_town     varchar(100),
  county        varchar(100),
  postcode      varchar(10),
  country       varchar(100)
);

create table pcs_case (
  id             uuid primary key,
  version        integer,
  case_reference bigint unique,
  address_id     uuid references address (id),
  general_notes  varchar(512)
);


create table possession_ground (
  id          uuid primary key,
  pcs_case_id uuid references pcs_case (id),
  code        varchar(50)
);



