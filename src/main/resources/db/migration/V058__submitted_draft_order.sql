-- A party filed this wording as the order it proposes. Rows are never changed: a correction is another
-- filing. The case is the party's.
CREATE TABLE submitted_draft_order (
    id            UUID PRIMARY KEY,
    party_id      UUID NOT NULL REFERENCES party(id) ON DELETE CASCADE,
    -- Rich text (HTML), cleaned when stored, as general_application.draft_order_wording.
    wording       TEXT NOT NULL,
    -- The IDAM id of the user who filed it.
    submitted_by  TEXT NOT NULL,
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX submitted_draft_order_party_id_idx ON submitted_draft_order (party_id);
