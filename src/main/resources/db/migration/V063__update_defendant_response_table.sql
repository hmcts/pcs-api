ALTER TABLE defendant_response
    ADD COLUMN sot_id UUID NOT NULL REFERENCES statement_of_truth(id), --found in claim, has 1-to-1 mapping
    ADD COLUMN pcs_case_id UUID NOT NULL REFERENCES pcs_case(id),--found in claim
    ADD COLUMN correspondence_address_confirmation YES_NO NOT NULL,
    ADD COLUMN possession_notice_received YES_NO,
    ADD COLUMN notice_received_date DATE,
    ADD COLUMN rent_arrears_amount_confirmation YES_NO,
    ADD COLUMN dispute_claim YES_NO NOT NULL,
    ADD COLUMN landlord_registered YES_NO NOT NULL,
    ADD COLUMN make_counter_claim YES_NO NOT NULL,
    ADD COLUMN version INT,
    ADD COLUMN status VARCHAR(60),
    ADD COLUMN response_submitted_date TIMESTAMP,
    ADD COLUMN response_deleted_date TIMESTAMP,
    ADD COLUMN response_received_date TIMESTAMP,
    ADD COLUMN language_used TEXT NOT NULL,
    ADD COLUMN channel VARCHAR(60),
    ADD COLUMN ingestion_source VARCHAR(60);
