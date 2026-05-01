ALTER TABLE fee_payment
  ADD COLUMN hwf_id UUID,
  ADD CONSTRAINT fk_fee_payment_help_with_fees FOREIGN KEY (hwf_id) REFERENCES help_with_fees (id);
