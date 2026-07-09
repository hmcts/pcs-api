DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'defendant_response'
          AND column_name = 'landlord_registered'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'defendant_response'
          AND column_name = 'exempt_landlord'
    ) THEN
        ALTER TABLE defendant_response
            RENAME COLUMN landlord_registered TO exempt_landlord;
    END IF;
END $$;
