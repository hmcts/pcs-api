-- Remove the superseded CLAIMANT_PACK_SENT / DEFENDANT_PACK_SENT values from claim_activity_type. All sends
-- are now recorded per (recipient, document) as DOCUMENT_SENT, nothing writes the old values, and no rows use
-- them (verified). Postgres cannot drop enum values in place, so the type is recreated.
ALTER TYPE claim_activity_type RENAME TO claim_activity_type_old;

CREATE TYPE claim_activity_type AS ENUM (
    'DOCUMENTS_CREATED',
    'DOCUMENT_SENT'
);

ALTER TABLE claim_activity_log
    ALTER COLUMN activity_type TYPE claim_activity_type
    USING activity_type::text::claim_activity_type;

DROP TYPE claim_activity_type_old;
