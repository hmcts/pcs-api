-- Pack-grained dispatch outcomes on claim_activity_log. The log stores events only -
-- document relationships stay in the document table and its owners
-- (claim.claim_form_document_id, defendant_response.submission_document_id, document.party_id).
--
--   DOCUMENTS_CREATED         : generation outcome per party; FAILURE rows carry
--                               GenerationDetails {documentType, failureReason, terminal} in details
--   PACK_SENT / PACK_FAILED   : one row per (recipient, pack) dispatch, with PackDetails
--                               {packType, documents:[{id,type}], failureReason, terminal} in details
ALTER TABLE claim_activity_log ADD COLUMN details JSONB;

-- Store activity_type/status as plain VARCHAR: the value sets are owned by the Java enums
-- (ClaimActivityType, ClaimActivityStatus), so adding/removing values is a code-only change -
-- no Postgres enum-type recreation dances. Drops the superseded pack-sent values with the type.
ALTER TABLE claim_activity_log ALTER COLUMN activity_type TYPE VARCHAR USING activity_type::text;
ALTER TABLE claim_activity_log ALTER COLUMN status TYPE VARCHAR USING status::text;
DROP TYPE claim_activity_type;
DROP TYPE claim_activity_status;

CREATE INDEX IF NOT EXISTS idx_claim_activity_log_type_status ON claim_activity_log (activity_type, status);
