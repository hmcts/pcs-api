-- Remove the superseded CLAIMANT_PACK_SENT / DEFENDANT_PACK_SENT values from claim_activity_type. All sends
-- are now recorded per (recipient, document) as DOCUMENT_SENT, nothing writes the old values, and no rows use
-- them (verified). Postgres cannot drop enum values in place; this recreates the type under the same name
-- (no rename) by detaching the column, dropping the type, and re-attaching. Runs in one transaction.
ALTER TABLE claim_activity_log ALTER COLUMN activity_type TYPE text USING activity_type::text;

DROP TYPE claim_activity_type;

CREATE TYPE claim_activity_type AS ENUM (
    'DOCUMENTS_CREATED',
    'DOCUMENT_SENT'
);

ALTER TABLE claim_activity_log
    ALTER COLUMN activity_type TYPE claim_activity_type USING activity_type::claim_activity_type;
