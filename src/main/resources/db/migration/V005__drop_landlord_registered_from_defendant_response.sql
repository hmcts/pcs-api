UPDATE defendant_response
SET exempt_landlord = landlord_registered
WHERE exempt_landlord IS NULL
  AND landlord_registered IS NOT NULL;

ALTER TABLE defendant_response
    DROP COLUMN landlord_registered;
