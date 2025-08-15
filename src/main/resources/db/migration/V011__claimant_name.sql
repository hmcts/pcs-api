ALTER TABLE public.pcs_case
DROP COLUMN  applicant_forename;

ALTER TABLE public.pcs_case
DROP COLUMN applicant_surname;


CREATE TABLE public.claim
(
    id      uuid primary key,
    version integer,
    case_id uuid references public.pcs_case (id),
    created timestamp with time zone,
    summary varchar(255),
    state   varchar(40)
);

CREATE TABLE public.claim_party
(
    version  integer,
    claim_id uuid references public.claim (id),
    party_id uuid references public.party (id),
    role     varchar(40) not null,

    primary key (claim_id, party_id)
);
