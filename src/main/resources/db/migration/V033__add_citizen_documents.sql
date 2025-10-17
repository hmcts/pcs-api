ALTER TABLE pcs_case
ADD COLUMN citizen_documents JSONB;
CREATE INDEX idx_pcs_case_citizen_documents
ON pcs_case USING GIN (citizen_documents);

-- Add comment
COMMENT ON COLUMN pcs_case.citizen_documents IS 'Documents uploaded from CUI';
