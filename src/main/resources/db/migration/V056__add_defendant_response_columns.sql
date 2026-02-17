ALTER TABLE defendant_response
    ADD COLUMN tenancy_type_correct YES_NO_NOT_SURE,
    ADD COLUMN tenancy_start_date_correct YES_NO_NOT_SURE,
    ADD COLUMN owe_rent_arrears YES_NO_NOT_SURE,
    ADD COLUMN rent_arrears_amount NUMERIC(19, 2),
    ADD COLUMN notice_received YES_NO_NOT_SURE,
    ADD COLUMN notice_received_date DATE;
