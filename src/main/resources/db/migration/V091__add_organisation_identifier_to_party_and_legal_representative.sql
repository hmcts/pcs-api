alter table legal_representative
    add column organisation_id varchar(64);

alter table party
    add column organisation_id varchar(64);
