-- Making a claim belongs to the organisation that owns the case, not to the person typing it. Group
-- access makes the case visible to the whole firm, so a colleague picking up a draft must see the
-- same answers rather than starting an empty journey of their own.
--
-- The owning party identifies the draft when set; when null the draft stays keyed on the user, which
-- is how the defendant response journey continues to keep a citizen and their legal representative
-- separate.
ALTER TABLE draft.draft_case_data ADD COLUMN owner_party_id uuid;

ALTER TABLE draft.draft_case_data
    ADD CONSTRAINT draft_case_data_owner_party_fk
    FOREIGN KEY (owner_party_id) REFERENCES public.party (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX draft_case_data_owner_party_key
    ON draft.draft_case_data (case_reference, event_id, owner_party_id)
    WHERE owner_party_id IS NOT NULL;
