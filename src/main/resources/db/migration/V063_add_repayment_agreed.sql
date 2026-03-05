CREATE TYPE YES_NO_NOT_SURE AS ENUM ('YES', 'NO', 'NOT_SURE');

ALTER TABLE defendant_response
    ADD COLUMN repayment_agreed YES_NO_NOT_SURE;
    ADD COLUMN repayment_agreed_details TEXT;
