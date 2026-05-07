-- Add claimant_type column to pcs_case table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'pcs_case' AND column_name = 'claimant_type') THEN
        ALTER TABLE pcs_case ADD COLUMN claimant_type VARCHAR(50);
    END IF;
END $$;
