-- flyway:executeInTransaction=false

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_claim_case_id
  ON claim (case_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_party_case_id
  ON party (case_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tenancy_licence_case_id
  ON tenancy_licence (case_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_note_case_id
  ON case_note (case_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_flag_pcs_case_id
  ON case_flag (pcs_case_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_party_flag_party_id
  ON case_party_flag (party_id);
