ALTER TABLE defendant_response
    ADD COLUMN owe_rent_arrears YES_NO_NOT_SURE,
    ADD COLUMN rent_arrears_amount NUMERIC(19, 2);
