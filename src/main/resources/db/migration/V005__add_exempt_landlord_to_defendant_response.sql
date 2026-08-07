ALTER TABLE defendant_response
    ADD COLUMN exempt_landlord YES_NO_NOT_SURE;

UPDATE defendant_response
SET exempt_landlord = landlord_registered
WHERE landlord_registered IS NOT NULL;
