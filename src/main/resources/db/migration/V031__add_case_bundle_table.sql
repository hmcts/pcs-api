-- One row per generated case bundle. The stitched PDF itself is a row in document (type
-- CASE_BUNDLE); this table holds what the bundle was built from and how, modelled on
-- em-stitching-api's BundleDTO. Bundles are regenerated whenever a document that belongs in one
-- is added to the case, so a case accumulates bundles and exactly one of them is latest.
CREATE TABLE case_bundle (
    id                       UUID PRIMARY KEY,
    case_id                  UUID         NOT NULL REFERENCES pcs_case (id),
    document_id              UUID         NOT NULL REFERENCES document (id),
    -- The document-bundling job (bundling.bundle_job.external_id) that rendered it.
    job_id                   UUID         NOT NULL UNIQUE,
    bundle_title             TEXT         NOT NULL,
    description              TEXT,
    file_name                TEXT         NOT NULL,
    stitch_status            VARCHAR(32)  NOT NULL,
    page_count               INTEGER      NOT NULL,
    sha256                   VARCHAR(64)  NOT NULL,
    -- The folder tree with each source document and where it landed in the PDF.
    folders                  JSONB        NOT NULL,
    warnings                 JSONB        NOT NULL DEFAULT '[]'::jsonb,
    has_table_of_contents    BOOLEAN      NOT NULL,
    has_coversheets          BOOLEAN      NOT NULL,
    has_folder_coversheets   BOOLEAN      NOT NULL,
    page_number_format       VARCHAR(32)  NOT NULL,
    -- When the job chose its documents; the latest bundle is the one chosen most recently.
    documents_selected_at    TIMESTAMPTZ  NOT NULL,
    latest                   BOOLEAN      NOT NULL DEFAULT false,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_case_bundle_case ON case_bundle (case_id, documents_selected_at DESC);
CREATE UNIQUE INDEX uq_case_bundle_latest ON case_bundle (case_id) WHERE latest;
