ALTER TABLE household_circumstances
    ADD COLUMN income_from_jobs YES_NO,
    ADD COLUMN income_from_jobs_amount DECIMAL(18,2),
    ADD COLUMN income_from_jobs_frequency VARCHAR(10),
    ADD COLUMN pension YES_NO,
    ADD COLUMN pension_amount DECIMAL(18,2),
    ADD COLUMN pension_frequency VARCHAR(10),
    ADD COLUMN universal_credit_amount DECIMAL(18,2),
    ADD COLUMN universal_credit_frequency VARCHAR(10),
    ADD COLUMN other_benefits YES_NO,
    ADD COLUMN other_benefits_amount DECIMAL(18,2),
    ADD COLUMN other_benefits_frequency VARCHAR(10),
    ADD COLUMN money_from_elsewhere YES_NO,
    ADD COLUMN money_from_elsewhere_details VARCHAR(500);
