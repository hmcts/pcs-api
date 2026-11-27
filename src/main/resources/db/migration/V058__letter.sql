-- A letter Send Letter accepted, and where it has got to. Send Letter hands back the same letter for an
-- identical request while that letter is still waiting to be uploaded, so one letter can answer more than
-- one PACK_SENT row; each of those rows points at it.
CREATE TABLE letter (
    letter_id        UUID PRIMARY KEY,
    -- LetterStatus: Send Letter's status (CREATED, UPLOADED, POSTED, ABORTED, ...).
    status           VARCHAR NOT NULL,
    sent_to_print_at TIMESTAMP,
    printed_at       TIMESTAMP,
    last_checked_at  TIMESTAMP
);

CREATE INDEX idx_letter_status ON letter (status);

ALTER TABLE claim_activity_log
    ADD COLUMN letter_id UUID REFERENCES letter (letter_id),
    ADD CONSTRAINT claim_activity_log_letter_only_on_pack_sent
        CHECK (letter_id IS NULL OR activity_type = 'PACK_SENT');

CREATE INDEX idx_claim_activity_log_letter_id ON claim_activity_log (letter_id);

-- Letters already sent; the status sync fills in where each one has got to.
INSERT INTO letter (letter_id, status)
SELECT DISTINCT (details ->> 'letterId')::uuid, 'CREATED'
FROM claim_activity_log
WHERE activity_type = 'PACK_SENT'
  AND status = 'SUCCESS'
  AND details ->> 'letterId' IS NOT NULL;

UPDATE claim_activity_log
SET letter_id = (details ->> 'letterId')::uuid
WHERE activity_type = 'PACK_SENT'
  AND status = 'SUCCESS'
  AND details ->> 'letterId' IS NOT NULL;
