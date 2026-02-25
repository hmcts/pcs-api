ALTER TABLE defendant_response
    ADD COLUMN IF NOT EXISTS owe_rent_arrears YES_NO_NOT_SURE,
    ADD COLUMN IF NOT EXISTS rent_arrears_amount NUMERIC(19, 2);
