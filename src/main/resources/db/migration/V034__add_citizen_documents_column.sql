-- Add citizen_documents column to pcs_case table
ALTER TABLE pcs_case
ADD COLUMN citizen_documents JSONB;

-- Add comment to describe the column purpose
COMMENT ON COLUMN pcs_case.citizen_documents IS 'Stores citizen uploaded document metadata and references in JSONB format';

-- Create index for better query performance on JSONB column
CREATE INDEX idx_pcs_case_citizen_documents ON pcs_case USING GIN (citizen_documents);
