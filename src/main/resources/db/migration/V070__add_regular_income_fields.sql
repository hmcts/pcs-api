ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs_frequency VARCHAR(60);

ALTER TABLE household_circumstances
ADD COLUMN pension YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN pension_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN pension_frequency VARCHAR(60);

-- Note: universal_credit YES_NO field already exists in V068
-- Adding amount and frequency fields to complement existing universal_credit field

ALTER TABLE household_circumstances
ADD COLUMN universal_credit_amount DECIMAL(18,2);

COMMENT ON COLUMN household_circumstances.universal_credit_amount IS 'Universal Credit income amount received - stored in pence';

ALTER TABLE household_circumstances
ADD COLUMN universal_credit_frequency VARCHAR(60);

COMMENT ON COLUMN household_circumstances.universal_credit_frequency IS 'Payment frequency: WEEK or MONTH';

ALTER TABLE household_circumstances
ADD COLUMN other_benefits YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN other_benefits_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN other_benefits_frequency VARCHAR(60);

ALTER TABLE household_circumstances
ADD COLUMN money_from_elsewhere YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN money_from_elsewhere_details VARCHAR(500);

