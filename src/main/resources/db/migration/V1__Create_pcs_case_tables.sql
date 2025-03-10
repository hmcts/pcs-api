-- create pcs_case table with reference as primary key
create table if not exists pcs_case (
    reference bigint primary key,
    case_description varchar(255) not null
);

create table if not exists party (
    id bigserial primary key,
    reference bigint references pcs_case(reference),
    forename varchar(255) not null,
    surname varchar(255) not null
);
