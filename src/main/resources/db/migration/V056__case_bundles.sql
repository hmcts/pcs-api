-- Every case bundle the SDK's doc toolkit hands over. Earlier bundles are kept: a judge may have
-- one open, or have made an order on it, and CDAM serves a document only while the case data
-- refers to it.
CREATE TABLE case_bundles (
    id                UUID PRIMARY KEY,
    case_id           UUID NOT NULL REFERENCES pcs_case(id) ON DELETE CASCADE,
    -- The case revision the bundle shows the case as at; the latest bundle has the highest.
    case_revision     BIGINT NOT NULL,
    url               TEXT NOT NULL,
    file_name         TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (case_id, case_revision)
);
