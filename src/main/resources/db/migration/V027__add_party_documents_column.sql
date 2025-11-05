-- Add party_documents column to pcs_case table
-- This column will store document metadata and references for party documents

ALTER TABLE pcs_case 
ADD COLUMN party_documents JSONB;

-- Add comment to describe the column purpose
COMMENT ON COLUMN pcs_case.party_documents IS 'Stores party document metadata and references in JSONB format';

-- Create index for better query performance on JSONB column
CREATE INDEX idx_pcs_case_party_documents ON pcs_case USING GIN (party_documents);
