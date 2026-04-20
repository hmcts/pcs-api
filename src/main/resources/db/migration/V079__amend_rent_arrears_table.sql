ALTER TABLE rent_arrears
  ADD COLUMN rent_arrears_recovery_attempted YES_NO,
  ADD COLUMN rent_arrears_recovery_attempt_details VARCHAR(500),
  DROP COLUMN third_party_payments_made;

DROP TABLE rent_arrears_payment_source;
