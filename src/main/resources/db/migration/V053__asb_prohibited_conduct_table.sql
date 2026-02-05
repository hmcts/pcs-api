CREATE TABLE asb_prohibited_conduct (
  id                                 UUID PRIMARY KEY,
  version                            INT,
  claim_id                           UUID REFERENCES claim (id),
  antisocial_behaviour               YES_NO,
  antisocial_behaviour_details       VARCHAR(500),
  illegal_purposes                   YES_NO,
  illegal_purposes_details           VARCHAR(500),
  other_prohibited_conduct           YES_NO,
  other_prohibited_conduct_details   VARCHAR(500),
  claiming_standard_contract         YES_NO,
  claiming_standard_contract_details VARCHAR(250),
  periodic_contract_agreed           YES_NO,
  periodic_contract_details          VARCHAR(250)
);

