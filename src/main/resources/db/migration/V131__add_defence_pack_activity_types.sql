-- Per-document send tracking: one DOCUMENT_SENT row per (recipient, document).
ALTER TYPE claim_activity_type ADD VALUE IF NOT EXISTS 'DOCUMENT_SENT';
ALTER TABLE claim_activity_log ADD COLUMN document_id UUID REFERENCES document(id);
CREATE INDEX IF NOT EXISTS idx_claim_activity_log_type_status ON claim_activity_log (activity_type, status);
