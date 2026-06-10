ALTER TABLE claim ADD COLUMN claim_issued_date TIMESTAMP WITH TIME ZONE;

ALTER TABLE fee_payment RENAME COLUMN payment_status TO status;
ALTER TABLE fee_payment RENAME COLUMN claim_id TO possession_claim_id;

