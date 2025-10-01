create table public.general_application (
  id      uuid primary key,
  version integer,
  case_id uuid references public.pcs_case (id),
  created timestamp with time zone,
  summary varchar(255),
  state   varchar(40)
);

create table public.general_application_party (
  version                integer,
  general_application_id uuid references public.general_application (id),
  party_id               uuid references public.party (id),

  primary key (general_application_id, party_id)
);
