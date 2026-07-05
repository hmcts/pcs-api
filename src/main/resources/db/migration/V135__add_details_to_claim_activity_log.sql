-- Structured outcome detail per activity row (shape varies by activity_type, see ActivityDetails):
--   DOCUMENTS_CREATED FAILURE -> GenerationDetails {documentType, failureReason, terminal, attempt}
--   PACK_SENT / PACK_FAILED   -> PackDetails {packType, documents, failureReason, terminal}
ALTER TABLE claim_activity_log ADD COLUMN details JSONB;

-- Pack-grained dispatch outcomes: one row per (recipient, pack) send. Appending enum values is safe
-- (unlike removal, cf. V134); DOCUMENT_SENT keeps being written in parallel until the selectors move
-- their dedup key onto pack coverage.
ALTER TYPE claim_activity_type ADD VALUE IF NOT EXISTS 'PACK_SENT';
ALTER TYPE claim_activity_type ADD VALUE IF NOT EXISTS 'PACK_FAILED';
