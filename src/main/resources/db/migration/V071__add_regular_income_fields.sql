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

ALTER TABLE household_circumstances
ADD COLUMN universal_credit_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN universal_credit_frequency VARCHAR(60);

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

