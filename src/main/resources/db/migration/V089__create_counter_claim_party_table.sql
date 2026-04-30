CREATE TABLE counter_claim_party (
  id       UUID PRIMARY KEY,
  cc_id    UUID NOT NULL REFERENCES counter_claim(id),
  party_id UUID NOT NULL REFERENCES party(id)
);

CREATE UNIQUE INDEX ux_counter_claim_party
  ON counter_claim_party(cc_id, party_id);

CREATE INDEX idx_counter_claim_party_party_id
  ON counter_claim_party(party_id);
