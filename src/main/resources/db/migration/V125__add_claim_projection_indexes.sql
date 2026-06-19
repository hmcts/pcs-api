-- flyway:executeInTransaction=false

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_claim_ground_claim_id
  ON claim_ground (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notice_of_possession_claim_id
  ON notice_of_possession (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_possession_alternatives_claim_id
  ON possession_alternatives (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_rent_arrears_claim_id
  ON rent_arrears (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_statement_of_truth_claim_id
  ON statement_of_truth (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_enf_case_claim_id
  ON enf_case (claim_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_enf_case_sot_id
  ON enf_case (sot_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fee_payment_possession_claim_id
  ON fee_payment (possession_claim_id);
