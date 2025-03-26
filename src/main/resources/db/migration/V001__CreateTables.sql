create table public.pcs_case
(
  id             uuid primary key,
  version        integer,
  case_reference bigint unique references ccd.case_data (reference) deferrable initially deferred
);

create table public.address
(
  id            uuid primary key,
  version       integer,
  case_id       uuid references public.pcs_case (id),
  address_line1 varchar(100),
  address_line2 varchar(100),
  address_line3 varchar(100),
  post_town     varchar(100),
  county        varchar(100),
  postcode      varchar(10),
  country       varchar(100)
);

create table public.party
(
  id       uuid primary key,
  version  integer,
  case_id  uuid references public.pcs_case (id),
  forename varchar(100),
  surname  varchar(100),
  active   boolean not null default true
);

create table public.claim
(
  id      uuid primary key,
  version integer,
  case_id uuid references public.pcs_case (id),
  created timestamp with time zone,
  summary varchar(255),
  state   varchar(40)
);

create table public.claim_party
(
  version  integer,
  claim_id uuid references public.claim (id),
  party_id uuid references public.party (id),
  role     varchar(40) not null,

  primary key (claim_id, party_id)
);
