-- Group access: the organisation that owns a case, and the draft on it.

-- The profile decides which access type an organisation gets, so it decides the capacity the case
-- is stamped with.
ALTER TABLE public.party ADD COLUMN organisation_profile_id varchar(64);

-- draft_case_data_unique_idx does not cover these rows: it includes party_id, and Postgres counts
-- NULLs as distinct, so it constrains nothing once party_id is null.
CREATE UNIQUE INDEX draft_case_data_organisation_key
    ON draft.draft_case_data (case_reference, event_id, organisation_id)
    WHERE organisation_id IS NOT NULL;
