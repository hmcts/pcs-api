ALTER TABLE defendant_response
    ADD COLUMN rent_arrears_amount_confirmation YES_NO_NOT_SURE,
    ADD COLUMN rent_arrears_amount NUMERIC(12, 2);
