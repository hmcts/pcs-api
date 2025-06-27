ALTER TABLE pcs_case
  ADD COLUMN tenancy_agreement_type VARCHAR(40),
  ADD COLUMN tenancy_start_date     DATE,
  ADD COLUMN mediation_attempted    BOOLEAN,
  ADD COLUMN settlement_attempted   BOOLEAN,
  ADD COLUMN rent_amount            NUMERIC(10, 2),
  ADD COLUMN rent_frequency         VARCHAR(20);
