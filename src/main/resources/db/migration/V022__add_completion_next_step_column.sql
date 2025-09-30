ALTER TABLE claim ADD COLUMN completion_next_step VARCHAR(255);

COMMENT ON COLUMN claim.completion_next_step IS 'The next step the user wants to take after completing their claim (SUBMIT_AND_PAY_NOW or SAVE_IT_FOR_LATER)';
