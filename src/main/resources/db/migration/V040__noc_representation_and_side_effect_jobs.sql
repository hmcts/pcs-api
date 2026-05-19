create table party_representation (
  id uuid primary key,
  case_reference bigint not null,
  party_id uuid not null references party (id),
  party_role text not null,
  organisation_id varchar(64) not null,
  organisation_name varchar(255),
  case_role varchar(64) not null,
  status varchar(32) not null,
  source varchar(32) not null,
  started_at timestamp without time zone not null,
  ended_at timestamp without time zone
);

create index idx_party_representation_case_role_status
  on party_representation (case_reference, case_role, status);

create index idx_party_representation_party_status
  on party_representation (party_id, status);

create unique index uq_party_representation_active_party
  on party_representation (case_reference, party_id)
  where status = 'ACTIVE';

create table noc_side_effect_job (
  id uuid primary key,
  case_reference bigint not null,
  party_id uuid not null,
  type varchar(64) not null,
  status varchar(32) not null,
  user_id varchar(64),
  organisation_id varchar(64),
  case_role varchar(64),
  email varchar(255),
  detail text,
  idempotency_key varchar(255) not null,
  attempts integer not null default 0,
  last_error text,
  created_at timestamp without time zone not null,
  available_at timestamp without time zone not null,
  completed_at timestamp without time zone
);

create unique index uq_noc_side_effect_job_idempotency
  on noc_side_effect_job (idempotency_key);

create index idx_noc_side_effect_job_status_available
  on noc_side_effect_job (status, available_at);
