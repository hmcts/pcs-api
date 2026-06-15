alter table legal_representative
    add column organisation_id varchar(64);

alter table party
    add column organisation_id varchar(64);

ALTER TABLE defendant_response
  ADD CONSTRAINT defendant_response_claim_party UNIQUE (claim_id, party_id);
