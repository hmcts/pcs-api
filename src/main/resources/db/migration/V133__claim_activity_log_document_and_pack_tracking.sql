-- Per-document generation tracking and pack-grained dispatch outcomes on claim_activity_log:
--   DOCUMENTS_CREATED rows carry the created document (document_id); FAILURE rows carry
--   GenerationDetails in details. PACK_SENT / PACK_FAILED record one row per (recipient, pack)
--   dispatch with PackDetails {packType, documents, failureReason, terminal} in details.
ALTER TABLE claim_activity_log ADD COLUMN document_id UUID REFERENCES document(id);
ALTER TABLE claim_activity_log ADD COLUMN details JSONB;

-- Recreate the enum at its final shape (drops the superseded, never-written CLAIMANT_PACK_SENT /
-- DEFENDANT_PACK_SENT values; Postgres cannot drop enum values in place). Column-swap keeps the
-- name and runs in one transaction.
ALTER TABLE claim_activity_log ALTER COLUMN activity_type TYPE text USING activity_type::text;
DROP TYPE claim_activity_type;
CREATE TYPE claim_activity_type AS ENUM (
    'DOCUMENTS_CREATED',
    'DOCUMENT_SENT',
    'PACK_SENT',
    'PACK_FAILED'
);
ALTER TABLE claim_activity_log
    ALTER COLUMN activity_type TYPE claim_activity_type USING activity_type::claim_activity_type;

CREATE INDEX IF NOT EXISTS idx_claim_activity_log_type_status ON claim_activity_log (activity_type, status);
