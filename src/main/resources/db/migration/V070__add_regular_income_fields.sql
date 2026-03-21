-- HDPI-3764: Add missing columns for regular income step
-- Following existing pattern: YES_NO for checkboxes, DECIMAL(18,2) for amounts (stored in pence), VARCHAR(60) for frequency
-- Stateless architecture: Every UI field must have corresponding database column for exact state reconstruction

-- Income from all jobs you do
ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN income_from_jobs_frequency VARCHAR(60);

-- Pension - state and private
ALTER TABLE household_circumstances
ADD COLUMN pension YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN pension_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN pension_frequency VARCHAR(60);

-- Universal Credit (income received - distinct from application question)
-- Note: Separate from existing 'universal_credit' YES_NO which tracks application status
-- Note: Amount and frequency excluded pending BA clarification - checkbox used for routing only
ALTER TABLE household_circumstances
ADD COLUMN universal_credit_income YES_NO;

-- Other benefits and credits
ALTER TABLE household_circumstances
ADD COLUMN other_benefits YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN other_benefits_amount DECIMAL(18,2);

ALTER TABLE household_circumstances
ADD COLUMN other_benefits_frequency VARCHAR(60);

-- Money from somewhere else
ALTER TABLE household_circumstances
ADD COLUMN money_from_elsewhere YES_NO;

ALTER TABLE household_circumstances
ADD COLUMN money_from_elsewhere_details VARCHAR(500);

-- Add comments for documentation
COMMENT ON COLUMN household_circumstances.income_from_jobs IS 'Checkbox: Does defendant receive income from jobs?';
COMMENT ON COLUMN household_circumstances.income_from_jobs_amount IS 'Total amount received from all jobs - stored in pence';
COMMENT ON COLUMN household_circumstances.income_from_jobs_frequency IS 'Payment frequency: WEEK or MONTH';

COMMENT ON COLUMN household_circumstances.pension IS 'Checkbox: Does defendant receive pension income?';
COMMENT ON COLUMN household_circumstances.pension_amount IS 'Total pension amount (state and private) - stored in pence';
COMMENT ON COLUMN household_circumstances.pension_frequency IS 'Payment frequency: WEEK or MONTH';

COMMENT ON COLUMN household_circumstances.universal_credit_income IS 'Checkbox: Does defendant receive Universal Credit? (routing only - amount/frequency excluded pending BA clarification)';

COMMENT ON COLUMN household_circumstances.other_benefits IS 'Checkbox: Does defendant receive other benefits/credits?';
COMMENT ON COLUMN household_circumstances.other_benefits_amount IS 'Total other benefits and credits amount - stored in pence';
COMMENT ON COLUMN household_circumstances.other_benefits_frequency IS 'Payment frequency: WEEK or MONTH';

COMMENT ON COLUMN household_circumstances.money_from_elsewhere IS 'Checkbox: Does defendant receive money from other sources?';
COMMENT ON COLUMN household_circumstances.money_from_elsewhere_details IS 'Description of other income sources and amounts (e.g., child maintenance)';

-- Note: The existing 'regular_income VARCHAR(60)' column can remain for backward compatibility
-- or be used as a denormalized cache, but these 12 new columns are the source of truth
--
-- Total columns added: 12
-- - 5 YES_NO checkboxes (income_from_jobs, pension, universal_credit_income, other_benefits, money_from_elsewhere)
-- - 3 DECIMAL amounts (income_from_jobs_amount, pension_amount, other_benefits_amount)
-- - 3 VARCHAR(60) frequencies (income_from_jobs_frequency, pension_frequency, other_benefits_frequency)
-- - 1 VARCHAR(500) details (money_from_elsewhere_details)
