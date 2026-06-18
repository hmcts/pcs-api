ALTER TABLE defendant_response
    ADD COLUMN tenancy_type VARCHAR(60),
    ADD COLUMN rent_arrears_amount DECIMAL(18,2);
