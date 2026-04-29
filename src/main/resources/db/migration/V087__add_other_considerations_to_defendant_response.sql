ALTER TABLE defendant_response
    ADD COLUMN other_considerations YES_NO,
    ADD COLUMN other_considerations_details VARCHAR(6400);
