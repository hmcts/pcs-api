ALTER TABLE statement_of_truth
    ALTER COLUMN full_name TYPE VARCHAR(100),
    ALTER COLUMN firm_name TYPE VARCHAR(100),
    ALTER COLUMN position_held TYPE VARCHAR(100);

ALTER TABLE enf_warrant
ALTER COLUMN full_name_claimant TYPE VARCHAR(100),
    ALTER COLUMN position_claimant TYPE VARCHAR(100),
    ALTER column full_name_legal_rep TYPE VARCHAR(100),
    ALTER COLUMN firm_name_legal_rep TYPE VARCHAR(100),
    ALTER COLUMN position_legal_rep TYPE VARCHAR(100);