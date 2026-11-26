-- Every version of a case's judges' bundle, as the SDK's doc toolkit hands it over. Versions are
-- kept: a judge may have an older one open, or have made an order on it, and CDAM serves a
-- document only while the case data refers to it.
CREATE TABLE case_bundle_versions (
    id                UUID PRIMARY KEY,
    case_id           UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    document_id       UUID NOT NULL UNIQUE,
    url               TEXT NOT NULL,
    binary_url        TEXT NOT NULL,
    file_name         TEXT NOT NULL,
    page_count        INTEGER NOT NULL,
    -- The documents that could not be included, each with its folder, page and reason.
    missing_documents JSONB NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX case_bundle_versions_latest_idx ON case_bundle_versions (case_id, created_at DESC);
