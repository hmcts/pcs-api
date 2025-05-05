create table public.pcs_case
(
  id              uuid primary key,
  version         integer,
  case_reference  bigint unique,
  description     varchar(255),
  applicant_name  varchar(255),
  respondent_name varchar(255)
);

