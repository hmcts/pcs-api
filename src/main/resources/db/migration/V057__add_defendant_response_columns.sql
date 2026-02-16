ALTER TABLE defendant_response
    ADD COLUMN tenancy_type_correct yes_no_not_sure,
    ADD COLUMN tenancy_start_date_correct yes_no_not_sure,
    ADD COLUMN owe_rent_arrears yes_no_not_sure,
    ADD COLUMN rent_arrears_amount NUMERIC(19, 2),
    ADD COLUMN notice_received yes_no_not_sure,
    ADD COLUMN notice_received_date DATE;
