ALTER TABLE defendant_response
    ADD COLUMN dispute_claim    YES_NO,
    ADD COLUMN dispute_claim_details VARCHAR(6800);
