-- claim_party PK leads on claim_id, so lookups by party_id alone aren't covered.
CREATE INDEX IF NOT EXISTS idx_party_id
    ON claim_party (party_id);
