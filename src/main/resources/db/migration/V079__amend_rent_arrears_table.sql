ALTER TABLE rent_arrears
  ADD COLUMN recovery_attempted YES_NO,
  ADD COLUMN recovery_attempt_details VARCHAR(500),
  DROP COLUMN third_party_payments_made;

DROP TABLE rent_arrears_payment_source;
