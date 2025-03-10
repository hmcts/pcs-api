-- create pcs_case table with reference as primary key
create table pcs_case (
    reference bigint primary key references ccd.case_data(reference) deferrable initially deferred,
    case_description varchar(255) not null
);

create table party (
    id bigserial primary key,
    reference bigint references pcs_case(reference),
    forename varchar(255) not null,
    surname varchar(255) not null
);
