-- A generated case bundle is a CASE_BUNDLE row in document, replaced in place each time the
-- bundle is regenerated. bundle_job_id is the document-bundling job (bundling.bundle_job) that
-- rendered it, whose report says what the bundle contains, where each document landed and which
-- documents are missing and why.
ALTER TABLE document ADD COLUMN bundle_job_id UUID;

CREATE UNIQUE INDEX uq_document_case_bundle ON document (case_id) WHERE type = 'CASE_BUNDLE';
