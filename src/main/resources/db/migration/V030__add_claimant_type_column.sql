-- Add claimant_type column to pcs_case table
ALTER TABLE pcs_case ADD COLUMN claimant_type VARCHAR(50);

-- Add comment to document the column
COMMENT ON COLUMN pcs_case.claimant_type IS 'The type of claimant (PRIVATE_LANDLORD, PROVIDER_OF_SOCIAL_HOUSING, COMMUNITY_LANDLORD, MORTGAGE_LENDER, OTHER)';

-- Create index for better query performance
CREATE INDEX idx_pcs_case_claimant_type ON pcs_case(claimant_type);
