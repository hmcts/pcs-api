-- Group access: the organisation that owns a case, and the draft on it.

-- The profile decides which access type an organisation gets, so it decides the capacity the case
-- is stamped with.
ALTER TABLE public.party ADD COLUMN organisation_profile_ids varchar(255)[];

-- A draft on a claim belongs to the organisation that owns the case rather than the person typing
-- it. When null the draft stays keyed on the user, which is how the defendant response journey keeps
-- a citizen and their legal representative separate.
ALTER TABLE draft.draft_case_data ADD COLUMN owner_party_id uuid;

ALTER TABLE draft.draft_case_data
    ADD CONSTRAINT draft_case_data_owner_party_fk
    FOREIGN KEY (owner_party_id) REFERENCES public.party (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX draft_case_data_owner_party_key
    ON draft.draft_case_data (case_reference, event_id, owner_party_id)
    WHERE owner_party_id IS NOT NULL;
